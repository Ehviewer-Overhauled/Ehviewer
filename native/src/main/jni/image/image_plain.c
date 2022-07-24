/*
 * Copyright 2016 Hippo Seven
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

#include <stdlib.h>
#include <string.h>

#include "image_plain.h"
#include "image_utils.h"
#include "log.h"

void* PLAIN_create(int32_t width, int32_t height, const void* data)
{
  IMAGE * plain = NULL;
  void* buffer = NULL;
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
