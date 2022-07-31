/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
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
    image->bufferLen = (int) (image->height * src->stride);
    image->buffer = malloc(image->bufferLen);
    if (image->isAnimated) {
        AImageDecoder_setInternallyHandleDisposePrevious(decoder, false);
        AImageDecoderFrameInfo *frameInfo = AImageDecoderFrameInfo_create();
        AImageDecoder_getFrameInfo(decoder, frameInfo);
        image->delay = (int) AImageDecoderFrameInfo_getDuration(frameInfo) / 1000000;
        AImageDecoderFrameInfo_delete(frameInfo);
    }
    AImageDecoder_decodeImage(decoder, image->buffer, src->stride, image->bufferLen);
    if (!image->isAnimated) {
        recycleSource(image->src);
        image->src = NULL;
    }
}

IMAGE *createFromFd(int fd) {
    AImageDecoder *decoder = NULL;
    if (AImageDecoder_createFromFd(fd, &decoder) != ANDROID_IMAGE_DECODER_SUCCESS)
        return NULL;
    IMAGE *image = calloc(1, sizeof(IMAGE));
    SOURCE *src = calloc(1, sizeof(SOURCE));
    src->decoder = decoder;
    src->fd = fd;
    decodeCommon(image, src);
    return image;
}

IMAGE *createFromAddr(void *addr, long size) {
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
    plain->bufferLen = (int) length;

    return plain;
}

void render(IMAGE *image, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height) {
    copy_pixels(image->buffer, image->width, image->height, src_x, src_y, dst, dst_w, dst_h, dst_x,
                dst_y, width, height);
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
