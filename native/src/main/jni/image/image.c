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

#include <android/data_space.h>
#include <stdlib.h>
#include <string.h>

#include "image.h"
#include "image_utils.h"

void decodeCommon(IMAGE *image, SOURCE *src) {
    image->src = src;
    AImageDecoder *decoder = src->decoder;
    image->isAnimated = AImageDecoder_isAnimated(decoder);
    AImageDecoder_setDataSpace(decoder, ADATASPACE_SRGB);
    AImageDecoder_setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);
    const AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    image->alpha =
            AImageDecoderHeaderInfo_getAlphaFlags(headerInfo) == ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
    src->stride = AImageDecoder_getMinimumStride(decoder);
    image->bufferLen = image->height * src->stride;
    image->buffer = malloc(image->bufferLen);
    if (image->isAnimated) {
        AImageDecoder_setInternallyHandleDisposePrevious(decoder, false);
        src->frameInfo = AImageDecoderFrameInfo_create();
        AImageDecoder_getFrameInfo(decoder, src->frameInfo);
    }
    AImageDecoder_decodeImage(decoder, image->buffer, src->stride, image->bufferLen);
    if (!image->isAnimated) {
        recycleSource(image->src);
        image->src = NULL;
    }
}

IMAGE *createFromFd(JNIEnv *env, int fd) {
    AImageDecoder *decoder;
    AImageDecoder_createFromFd(fd, &decoder);
    IMAGE *image = calloc(1, sizeof(IMAGE));
    SOURCE *src = calloc(1, sizeof(SOURCE));
    src->decoder = decoder;
    src->fd = fd;
    decodeCommon(image, src);
    return image;
}

IMAGE *createFromAddr(JNIEnv *env, void *addr, long size) {
    AImageDecoder *decoder;
    AImageDecoder_createFromBuffer((const void *) addr, size, &decoder);
    IMAGE *image = calloc(1, sizeof(IMAGE));
    SOURCE *src = calloc(1, sizeof(SOURCE));
    src->decoder = decoder;
    src->srcBuffer = addr;
    decodeCommon(image, src);
    return image;
}

IMAGE *create(int32_t width, int32_t height, const void *data) {
    IMAGE *plain = calloc(1, sizeof(IMAGE));
    void *buffer = NULL;
    size_t length = width * height * 4;

    buffer = malloc(length);
    memcpy(buffer, data, length);

    plain->width = width;
    plain->height = height;
    plain->buffer = buffer;
    plain->bufferLen = length;

    return plain;
}

void render(IMAGE *image, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height, bool fill_blank, int default_color) {
    copy_pixels(image->buffer, image->width, image->height, src_x, src_y, dst, dst_w, dst_h, dst_x,
                dst_y, width, height, fill_blank, default_color);
}

void advance(IMAGE *image) {
    if (image->isAnimated) {
        AImageDecoder_advanceFrame(image->src->decoder);
        if (AImageDecoder_decodeImage(image->src->decoder, image->buffer, image->src->stride,
                                      image->bufferLen) == ANDROID_IMAGE_DECODER_FINISHED) {
            AImageDecoder_rewind(image->src->decoder);
            AImageDecoder_decodeImage(image->src->decoder, image->buffer, image->src->stride,
                                      image->bufferLen);
        }
    }
}

int get_delay(IMAGE *image) {
    if (image->isAnimated)
        return (int) AImageDecoderFrameInfo_getDuration(image->src->frameInfo) / 1000000;
    return 0;
}
