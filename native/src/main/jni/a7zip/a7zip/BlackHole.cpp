/*
 * Copyright 2019 Hippo Seven
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

#include "BlackHole.h"

using namespace a7zip;

BlackHole::BlackHole(): pos(0), size(0) { }

HRESULT BlackHole::Write(const void*, UInt32 size, UInt32* processedSize) {
  if (processedSize != nullptr) {
    *processedSize = size;
  }

  this->pos += size;
  if (this->size < this->pos) {
    this->size = this->pos;
  }

  return S_OK;
}

HRESULT BlackHole::Seek(Int64 offset, UInt32 seekOrigin, UInt64* newPosition) {
  Int64 newPos = -1;

  switch (seekOrigin) {
    case STREAM_SEEK_SET:
      newPos = offset;
      break;
    case STREAM_SEEK_CUR:
      newPos = this->pos + offset;
      break;
    case STREAM_SEEK_END:
      newPos = this->size + offset;
      break;
    default:
      return E_INVALIDARG;
  }

  if (newPos < 0) {
    return STG_E_INVALIDFUNCTION;
  }

  *newPosition = this->pos;

  return S_OK;
}

HRESULT BlackHole::SetSize(UInt64 newSize) {
  this->size = newSize;
  if (this->pos > this->size) {
    this->pos = this->size;
  }

  return S_OK;
}
