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

#include <android/imagedecoder.h>
#include <stdlib.h>
#include <unistd.h>

#include "image.h"
#include "image_plain.h"
#include "image_utils.h"
#include "filter/clahe.h"
#include "filter/gray.h"
#include "log.h"

void* decode(JNIEnv* env, int fd, bool partially, int* format)
{
    AImageDecoder *decoder;
    int r = AImageDecoder_createFromFd(fd, &decoder);
    LOGD("%s%d", "Create ImageDecoder with ret ", r);
    if (r)
        return NULL;
    *format = AImageDecoder_isAnimated(decoder);
    const AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    size_t stride = AImageDecoder_getMinimumStride(decoder);
    IMAGE *image = calloc(1, sizeof(IMAGE));
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    image->decoder = decoder;
    image->bufferLen = image->height * stride;
    image->buffer = malloc(image->bufferLen);
    AImageDecoder_decodeImage(decoder, image->buffer, stride, image->bufferLen);
    return image;
}

void* decodeAddr(JNIEnv* env, void* addr, long size, bool partially, int* format)
{
    AImageDecoder *decoder;
    int r = AImageDecoder_createFromBuffer((const void *) addr, size, &decoder);
    LOGD("%s%d", "Create ImageDecoder with ret ", r);
    if (r)
        return NULL;
    *format = AImageDecoder_isAnimated(decoder);
    const AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    size_t stride = AImageDecoder_getMinimumStride(decoder);
    IMAGE *image = calloc(1, sizeof(IMAGE));
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    image->decoder = decoder;
    image->bufferLen = image->height * stride;
    image->buffer = malloc(image->bufferLen);
    image->srcBuffer = addr;
    AImageDecoder_decodeImage(decoder, image->buffer, stride, image->bufferLen);
    return image;
}

void* create(int32_t width, int32_t height, const void* data)
{
    return PLAIN_create(width, height, data);
}

bool complete(JNIEnv* env, IMAGE * image, int format)
{
    return true;
}

bool is_completed(IMAGE * image, int format)
{
    return true;
}

int get_width(IMAGE * image, int format)
{
    return image->width;
}

int get_height(IMAGE * image, int format)
{
    return image->height;
}

int get_byte_count(IMAGE * image, int format)
{
    return image->bufferLen;
}

void render(IMAGE * image, int format, int src_x, int src_y,
    void* dst, int dst_w, int dst_h, int dst_x, int dst_y,
    int width, int height, bool fill_blank, int default_color)
{
    copy_pixels(image->buffer, image->width, image->height, src_x, src_y, dst, dst_w, dst_h, dst_x, dst_y, width, height, fill_blank, default_color);
}

void advance(IMAGE * image, int format)
{
    if (format) {
        AImageDecoder_advanceFrame(image->decoder);
    }
}

int get_delay(IMAGE * image, int format)
{
    return 0;
}

int get_frame_count(IMAGE * image, int format)
{
    return 1;
}

bool is_opaque(IMAGE * image, int format)
{
    return false;
}

static void get_image_data(IMAGE * image, int format, void** pixel, int* width, int* height) {
      *pixel = image->buffer;
      *width = image->width;
      *height = image->height;
}

bool is_gray(IMAGE * image, int format, int error)
{
  void* pixel = NULL;
  int width = 0;
  int height = 0;

  get_image_data(image, format, &pixel, &width, &height);

  if (pixel == NULL || width == 0 || height == 0) {
    return false;
  }

  return IMAGE_is_gray(pixel, width, height, error);
}

void clahe(IMAGE * image, int format, bool to_gray)
{
  void* pixel = NULL;
  int width = 0;
  int height = 0;

  get_image_data(image, format, &pixel, &width, &height);

  if (pixel == NULL || width == 0 || height == 0) {
    return;
  }

  IMAGE_clahe(pixel, width, height, to_gray);
}

void recycle(JNIEnv *env, IMAGE* image, int format)
{
    free(image->buffer);
    image->buffer = NULL;
    AImageDecoder_delete(image->decoder);
    free(image->srcBuffer);
    free(image);
}
