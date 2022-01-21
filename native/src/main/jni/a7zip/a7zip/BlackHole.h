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

#ifndef __A7ZIP_BLACK_HOLE_H__
#define __A7ZIP_BLACK_HOLE_H__

#include <Common/MyCom.h>
#include <7zip/IStream.h>

#include "Log.h"

namespace a7zip {

class BlackHole :
    public IOutStream,
    public CMyUnknownImp
{
 public:
  BlackHole();

 public:
  MY_UNKNOWN_IMP2(ISequentialOutStream, IOutStream)
  STDMETHOD(Write)(const void *data, UInt32 size, UInt32 *processedSize);
  STDMETHOD(Seek)(Int64 offset, UInt32 seekOrigin, UInt64 *newPosition);
  STDMETHOD(SetSize)(UInt64 newSize);

 private:
  UInt64 pos;
  UInt64 size;
};

}

#endif //__A7ZIP_BLACK_HOLE_H__
