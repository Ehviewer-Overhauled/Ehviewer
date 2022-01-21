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

#include "OpenVolumeCallback.h"

#include <malloc.h>

#include <MyString.h>

#include "SeekableInputStream.h"
#include "JavaEnv.h"
#include "Utils.h"
#include "Log.h"

using namespace a7zip;

bool OpenVolumeCallback::initialized = false;
jmethodID OpenVolumeCallback::method_open_volume = nullptr;

OpenVolumeCallback::OpenVolumeCallback(jobject callback) : callback(callback) { }

OpenVolumeCallback::~OpenVolumeCallback() {
  JavaEnv env;
  if (!env.IsValid()) return;

  env->DeleteGlobalRef(callback);
  callback = nullptr;
}

HRESULT OpenVolumeCallback::OpenVolume(const wchar_t *name, CMyComPtr<IInStream>& in_stream) {
  JavaEnv env;

  // const wchar_t* to jstring
  unsigned len = MyStringLen(name);
  jchar *buffer = reinterpret_cast<jchar *>(malloc(len * sizeof(jchar)));
  if (buffer == nullptr) {
    return E_OUTOFMEMORY;
  }
  for (int i = 0; i < len; i++) {
    buffer[i] = static_cast<jchar>(name[i]);
  }
  jstring j_name = env->NewString(buffer, len);
  free(buffer);
  if (j_name == nullptr) {
    return E_OUTOFMEMORY;
  }

  // Wrap java stream
  jobject stream = env->CallObjectMethod(callback, method_open_volume, j_name);
  RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);
  RETURN_SAME_IF_NOT_ZERO(SeekableInputStream::Create(static_cast<JNIEnv*>(env), stream, in_stream));
  return S_OK;
}

HRESULT OpenVolumeCallback::Initialize(JNIEnv* env) {
  if (initialized) {
    return S_OK;
  }

  jclass clazz = env->FindClass("com/hippo/a7zip/InArchive$OpenVolumeCallback");
  if (clazz == nullptr) return E_CLASS_NOT_FOUND;

  method_open_volume = env->GetMethodID(clazz, "openVolume", "(Ljava/lang/String;)Lcom/hippo/a7zip/SeekableInputStream;");
  if (method_open_volume == nullptr) return E_METHOD_NOT_FOUND;

  initialized = true;
  return S_OK;
}

HRESULT OpenVolumeCallback::Create(
    JNIEnv* env,
    jobject callback,
    OpenVolumeCallback** result
) {
  if (!initialized) {
    return E_NOT_INITIALIZED;
  }

  jobject g_callback = env->NewGlobalRef(callback);
  if (g_callback == nullptr) {
    return E_OUTOFMEMORY;
  }

  *result = new OpenVolumeCallback(g_callback);

  return S_OK;
}

HRESULT OpenVolumeCallback::Create(
    JNIEnv* env,
    jobject callback,
    CMyComPtr<OpenVolumeCallback>& result
) {
  OpenVolumeCallback *callback_ptr = nullptr;
  RETURN_SAME_IF_NOT_ZERO(Create(env, callback, &callback_ptr));
  result = callback_ptr;
  return S_OK;
}
