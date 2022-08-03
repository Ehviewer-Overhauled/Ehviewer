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

#include "image_utils.h"

#include <stdlib.h>
#include <string.h>

bool copy_pixels(const void *src, int src_w, int src_h, int src_x, int src_y,
                 void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
                 int width, int height) {
    int left;
    int line;
    size_t line_stride;
    int src_stride;
    int src_pos;
    int dst_pos;
    size_t dst_blank_length;

    // Sanitize
    if (src_x < 0) {
        width -= src_x;
        dst_x -= src_x;
        src_x = 0;
    }
    if (dst_x < 0) {
        width -= dst_x;
        src_x -= dst_x;
        dst_x = 0;
    }
    if (width <= 0) {
        return false;
    }
    if (src_y < 0) {
        height -= src_y;
        dst_y -= src_y;
        src_y = 0;
    }
    if (dst_y < 0) {
        height -= dst_y;
        src_y -= dst_y;
        dst_y = 0;
    }
    if (height <= 0) {
        return false;
    }
    left = src_x + width - src_w;
    if (left > 0) {
        width -= left;
    }
    left = dst_x + width - dst_w;
    if (left > 0) {
        width -= left;
    }
    if (width <= 0) {
        return false;
    }
    left = src_y + height - src_h;
    if (left > 0) {
        height -= left;
    }
    left = dst_y + height - dst_h;
    if (left > 0) {
        height -= left;
    }
    if (height <= 0) {
        return false;
    }

    // Init
    line_stride = (size_t) (width * 4);
    src_stride = src_w * 4;
    src_pos = src_y * src_stride + src_x * 4;
    dst_pos = 0;

    dst_blank_length = (size_t) (dst_y * dst_w + dst_x) * 4;

    // First line
    dst_pos += dst_blank_length;
    memcpy(dst + dst_pos, src + src_pos, line_stride);
    dst_pos += line_stride;
    src_pos += src_stride;

    // Other lines
    dst_blank_length = (size_t) ((dst_w - width) * 4);
    for (line = 1; line < height; line++) {
        dst_pos += dst_blank_length;
        memcpy(dst + dst_pos, src + src_pos, line_stride);
        dst_pos += line_stride;
        src_pos += src_stride;
    }

    return true;
}
