/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// Created by Hippo on 12/27/2015.
//

#include <android/data_space.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "image.h"
#include "image_utils.h"
#include "filter/clahe.h"
#include "filter/gray.h"
#include "log.h"

void decodeCommon(IMAGE *image) {
    AImageDecoder *decoder = image->decoder;
    AImageDecoder_setDataSpace(decoder, ADATASPACE_SRGB);
    AImageDecoder_setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);
    const AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    image->alpha =
            AImageDecoderHeaderInfo_getAlphaFlags(headerInfo) == ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
    image->stride = AImageDecoder_getMinimumStride(decoder);
    image->bufferLen = image->height * image->stride;
    image->buffer = malloc(image->bufferLen);
    if (image->isAnimated) {
        AImageDecoder_setInternallyHandleDisposePrevious(decoder, false);
        image->frameInfo = AImageDecoderFrameInfo_create();
        AImageDecoder_getFrameInfo(decoder, image->frameInfo);
    }
    AImageDecoder_decodeImage(decoder, image->buffer, image->stride, image->bufferLen);
}

IMAGE *createFromFd(JNIEnv *env, int fd, bool partially, int *format) {
    AImageDecoder *decoder;
    int r = AImageDecoder_createFromFd(fd, &decoder);
    LOGD("%s%d", "Create ImageDecoder with ret ", r);
    if (r)
        return NULL;
    IMAGE *image = calloc(1, sizeof(IMAGE));
    image->isAnimated = *format = AImageDecoder_isAnimated(decoder);
    image->decoder = decoder;
    decodeCommon(image);
    return image;
}

IMAGE *createFromAddr(JNIEnv *env, void *addr, long size, bool partially, int *format) {
    AImageDecoder *decoder;
    int r = AImageDecoder_createFromBuffer((const void *) addr, size, &decoder);
    LOGD("%s%d", "Create ImageDecoder with ret ", r);
    if (r)
        return NULL;
    IMAGE *image = calloc(1, sizeof(IMAGE));
    image->isAnimated = *format = AImageDecoder_isAnimated(decoder);
    image->decoder = decoder;
    image->srcBuffer = addr;
    decodeCommon(image);
    return image;
}

void *create(int32_t width, int32_t height, const void *data) {
    IMAGE *plain = NULL;
    void *buffer = NULL;
    size_t length;

    plain = malloc(sizeof(IMAGE));
    if (plain == NULL) {
        WTF_OM;
        return NULL;
    }

    length = (size_t) (width * height * 4);

    buffer = malloc(length);
    if (buffer == NULL) {
        WTF_OM;
        free(plain);
        return NULL;
    }

    memcpy(buffer, data, length);

    plain->width = width;
    plain->height = height;
    plain->buffer = buffer;
    plain->bufferLen = length;
    plain->decoder = NULL;

    return plain;
}

int get_width(IMAGE *image, int format) {
    return image->width;
}

int get_height(IMAGE *image, int format) {
    return image->height;
}

int get_byte_count(IMAGE *image, int format) {
    return image->bufferLen;
}

void render(IMAGE *image, int format, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height, bool fill_blank, int default_color) {
    copy_pixels(image->buffer, image->width, image->height, src_x, src_y, dst, dst_w, dst_h, dst_x,
                dst_y, width, height, fill_blank, default_color);
}

void advance(IMAGE *image, int format) {
    if (format) {
        AImageDecoder_advanceFrame(image->decoder);
        if (AImageDecoder_decodeImage(image->decoder, image->buffer, image->stride,
                                      image->bufferLen) == ANDROID_IMAGE_DECODER_FINISHED) {
            AImageDecoder_rewind(image->decoder);
            AImageDecoder_decodeImage(image->decoder, image->buffer, image->stride,
                                      image->bufferLen);
        }
    }
}

int get_delay(IMAGE *image, int format) {
    if (format)
        return AImageDecoderFrameInfo_getDuration(image->frameInfo) / 1000000;
    return 0;
}

bool is_opaque(IMAGE *image, int format) {
    return image->alpha;
}

static void get_image_data(IMAGE *image, int format, void **pixel, int *width, int *height) {
    *pixel = image->buffer;
    *width = image->width;
    *height = image->height;
}

bool is_gray(IMAGE *image, int format, int error) {
    void *pixel = NULL;
    int width = 0;
    int height = 0;

    get_image_data(image, format, &pixel, &width, &height);

    if (pixel == NULL || width == 0 || height == 0) {
        return false;
    }

    return IMAGE_is_gray(pixel, width, height, error);
}

void clahe(IMAGE *image, int format, bool to_gray) {
    void *pixel = NULL;
    int width = 0;
    int height = 0;

    get_image_data(image, format, &pixel, &width, &height);

    if (pixel == NULL || width == 0 || height == 0) {
        return;
    }

    IMAGE_clahe(pixel, width, height, to_gray);
}

void recycle(JNIEnv *env, IMAGE *image, int format) {
    free(image->buffer);
    image->buffer = NULL;
    AImageDecoderFrameInfo_delete(image->frameInfo);
    AImageDecoder_delete(image->decoder);
    free(image->srcBuffer);
    free(image);
}
