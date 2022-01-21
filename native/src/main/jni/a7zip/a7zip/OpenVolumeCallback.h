/*
 * Copyright 2020 Hippo Seven
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

#ifndef __A7ZIP_OPEN_VOLUME_CALLBACK_H__
#define __A7ZIP_OPEN_VOLUME_CALLBACK_H__

#include <jni.h>

#include <Common/MyCom.h>
#include <7zip/IStream.h>

namespace a7zip {

class OpenVolumeCallback : public CMyUnknownImp
{
 private:
  OpenVolumeCallback(jobject callback);
 public:
  virtual ~OpenVolumeCallback();

 public:
  MY_ADDREF_RELEASE

  HRESULT OpenVolume(const wchar_t *name, CMyComPtr<IInStream>& in_stream);

 private:
  jobject callback;

 public:
  static HRESULT Initialize(JNIEnv* env);
  static HRESULT Create(JNIEnv* env, jobject callback, OpenVolumeCallback** result);
  static HRESULT Create(JNIEnv* env, jobject callback, CMyComPtr<OpenVolumeCallback>& result);

 private:
  static bool initialized;
  static jmethodID method_open_volume;
};

}

#endif //__A7ZIP_OPEN_VOLUME_CALLBACK_H__
