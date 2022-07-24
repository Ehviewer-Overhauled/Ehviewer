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

#include "image.h"
#include "image_plain.h"
#include "filter/clahe.h"
#include "filter/gray.h"
#include "../log.h"

void* decode(JNIEnv* env, InputStream* stream, bool partially, int* format)
{
    AImageDecoder *decoder;
    size_t len = 1024 * 1024 * 4;
    void* buffer = calloc(len, sizeof(char));
    len = read_input_stream(env, stream, buffer, 0, len);
    AImageDecoder_createFromBuffer(buffer, len, &decoder);
    free(buffer);
    AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    IMAGE *image = malloc(sizeof(IMAGE));
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    ssize_t bufferLen = 1024 * 1024 * 64;
    image->buffer = malloc(bufferLen);
    AImageDecoder_decodeImage(decoder, image->buffer, AImageDecoder_getMinimumStride(decoder), bufferLen);
    AImageDecoder_delete(decoder);
    destroy_input_stream(env, &stream);
    *format = IMAGE_FORMAT_JPEG;
    return image;
}

void* create(unsigned int width, unsigned int height, const void* data)
{
    return PLAIN_create(width, height, data);
}

bool complete(JNIEnv* env, void* image, int format)
{
    return true;
}

bool is_completed(void* image, int format)
{
    return true;
}

int get_width(void* image, int format)
{
    return ((IMAGE*)image)->width;
}

int get_height(void* image, int format)
{
    return ((IMAGE*)image)->height;
}

int get_byte_count(void* image, int format)
{
    return ((IMAGE*)image)->width * ((IMAGE*)image)->height * 4;
}

void render(void* image, int format, int src_x, int src_y,
    void* dst, int dst_w, int dst_h, int dst_x, int dst_y,
    int width, int height, bool fill_blank, int default_color)
{
    PLAIN_render((IMAGE *) image, src_x, src_y, dst, dst_w, dst_h, dst_x, dst_y, width, height, fill_blank, default_color);
}

void advance(void* image, int format)
{
}

int get_delay(void* image, int format)
{
    return 0;
}

int get_frame_count(void* image, int format)
{
    return 1;
}

bool is_opaque(void* image, int format)
{
    return false;
}

static void get_image_data(void* image, int format, void** pixel, int* width, int* height) {
      PLAIN* plain = (PLAIN*) image;
      *pixel = PLAIN_get_pixels(plain);
      *width = PLAIN_get_width(plain);
      *height = PLAIN_get_height(plain);
}

bool is_gray(void* image, int format, int error)
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

void clahe(void* image, int format, bool to_gray)
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

void recycle(JNIEnv *env, void* image, int format)
{
    PLAIN_recycle((PLAIN*) image);
}

int get_supported_formats(int *formats)
{
  int i = 0;
#ifdef IMAGE_SUPPORT_JPEG
  formats[i++] = IMAGE_FORMAT_JPEG;
#endif
#ifdef IMAGE_SUPPORT_PNG
  formats[i++] = IMAGE_FORMAT_PNG;
#endif
#ifdef IMAGE_SUPPORT_GIF
  formats[i++] = IMAGE_FORMAT_GIF;
#endif
  return i;
}

const char *get_decoder_description(int format)
{
    return NULL;
}
