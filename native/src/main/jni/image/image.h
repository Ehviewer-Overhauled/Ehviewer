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

#ifndef IMAGE_IMAGE_H
#define IMAGE_IMAGE_H

#include <android/imagedecoder.h>
#include <stdbool.h>

typedef struct {
    AImageDecoder *decoder;
    void *srcBuffer;
    size_t stride;
    int fd;
} SOURCE;

typedef struct {
    int32_t width;
    int32_t height;
    void *buffer;
    int bufferLen;
    bool isAnimated;
    bool alpha;
    int delay;
    SOURCE *src;
} IMAGE;

static void recycleSource(SOURCE *src) {
    if (!src)
        return;
    AImageDecoder_delete(src->decoder);
    free(src->srcBuffer);
    free(src);
}

static void recycle(IMAGE *image) {
    if (!image)
        return;
    recycleSource(image->src);
    free(image->buffer);
    free(image);
}

IMAGE *createFromFd(int fd);

IMAGE *createFromAddr(void *addr, long size);

IMAGE *create(int32_t width, int32_t height, const void *data);

void render(IMAGE *image, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height);

void advance(IMAGE *image);

#endif /* IMAGE_IMAGE_H */
