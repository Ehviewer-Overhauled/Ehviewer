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

#ifndef IMAGE_IMAGE_H
#define IMAGE_IMAGE_H

#include <android/imagedecoder.h>
#include <stdbool.h>
#include <jni.h>

typedef struct
{
    int32_t width;
    int32_t height;
    void* buffer;
    int bufferLen;
    AImageDecoder* decoder;
    void* srcBuffer;
} IMAGE;

void* decode(JNIEnv* env, int fd, bool partially, int* format);
void* decodeAddr(JNIEnv* env, void* addr, long size, bool partially, int* format);
void* create(int32_t width, int32_t height, const void* data);
bool complete(JNIEnv* env, IMAGE * image, int format);
bool is_completed(IMAGE * image, int format);
int get_width(IMAGE * image, int format);
int get_height(IMAGE * image, int format);
int get_byte_count(IMAGE * image, int format);
void render(IMAGE * image, int format, int src_x, int src_y,
    void* dst, int dst_w, int dst_h, int dst_x, int dst_y,
    int width, int height, bool fill_blank, int default_color);
void advance(IMAGE * image, int format);
int get_delay(IMAGE * image, int format);
int get_frame_count(IMAGE * image, int format);
bool is_opaque(IMAGE * image, int format);
bool is_gray(IMAGE * image, int format, int error);
void clahe(IMAGE * image, int format, bool to_gray);
void recycle(JNIEnv *env, IMAGE * image, int format);

#endif //IMAGE_IMAGE_H
