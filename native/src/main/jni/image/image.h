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

#ifndef IMAGE_IMAGE_H
#define IMAGE_IMAGE_H

#include <android/imagedecoder.h>
#include <stdbool.h>
#include <jni.h>

typedef struct {
    AImageDecoder *decoder;
    void *srcBuffer;
    size_t stride;
    AImageDecoderFrameInfo *frameInfo;
    int fd;
} SOURCE;

typedef struct {
    int32_t width;
    int32_t height;
    void *buffer;
    int bufferLen;
    bool isAnimated;
    bool alpha;
    SOURCE *src;
} IMAGE;

static void recycleSource(SOURCE *src) {
    if (!src)
        return;
    AImageDecoderFrameInfo_delete(src->frameInfo);
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

IMAGE *createFromFd(JNIEnv *env, int fd);

IMAGE *createFromAddr(JNIEnv *env, void *addr, long size);

IMAGE *create(int32_t width, int32_t height, const void *data);

void render(IMAGE *image, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height, bool fill_blank, int default_color);

void advance(IMAGE *image);

int get_delay(IMAGE *image);

#endif //IMAGE_IMAGE_H
