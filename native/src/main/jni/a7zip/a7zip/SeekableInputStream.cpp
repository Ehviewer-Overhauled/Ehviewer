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

#include <assert.h>
#include "SeekableInputStream.h"

#include "JavaEnv.h"
#include "Utils.h"
#include "Log.h"

#define ARRAY_SIZE DEFAULT_BUFFER_SIZE

using namespace a7zip;

bool SeekableInputStream::initialized = false;
jmethodID SeekableInputStream::method_read = nullptr;
jmethodID SeekableInputStream::method_seek = nullptr;
jmethodID SeekableInputStream::method_tell = nullptr;
jmethodID SeekableInputStream::method_size = nullptr;
jmethodID SeekableInputStream::method_close = nullptr;

SeekableInputStream::SeekableInputStream(
    jobject stream,
    jbyteArray array
) :
    stream(stream),
    array(array) { }

SeekableInputStream::~SeekableInputStream() {
  JavaEnv env;
  if (!env.IsValid()) return;

  env->CallVoidMethod(stream, method_close);
  CLEAR_IF_EXCEPTION_PENDING(env);

  env->DeleteGlobalRef(stream);
  env->DeleteGlobalRef(array);
  stream = nullptr;
  array = nullptr;
}

HRESULT SeekableInputStream::Read(void* data, UInt32 size, UInt32* processedSize) {
  if (processedSize != nullptr) {
    *processedSize = 0;
  }

  if (size == 0) {
    return S_OK;
  }

  JavaEnv env;
  if (!env.IsValid()) return E_JAVA_EXCEPTION;

  // Make size not bigger than ARRAY_SIZE
  size = MIN(ARRAY_SIZE, size);

  jint read = env->CallIntMethod(stream, method_read, array, 0, size);
  RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);

  // Check EOF
  if (read <= 0) {
    return S_OK;
  }

  env->GetByteArrayRegion(array, 0, read, static_cast<jbyte*>(data));
  RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);

  if (processedSize != nullptr) {
    *processedSize = static_cast<UInt32>(read);
  }

  return S_OK;
}

HRESULT SeekableInputStream::Seek(Int64 offset, UInt32 seekOrigin, UInt64* newPosition) {
  JavaEnv env;
  if (!env.IsValid()) return E_JAVA_EXCEPTION;

  jlong actual_offset;

  switch (seekOrigin) {
    case STREAM_SEEK_SET: {
      actual_offset = static_cast<jlong>(offset);
      break;
    }
    case STREAM_SEEK_CUR: {
      jlong position = env->CallLongMethod(stream, method_tell);
      RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);
      actual_offset = position + offset;
      break;
    }
    case STREAM_SEEK_END: {
      jlong size = env->CallLongMethod(stream, method_size);
      RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);
      actual_offset = size + offset;
      break;
    }
    default: {
      return E_INVALIDARG;
    }
  }

  if (actual_offset < 0) {
    return E_INVALIDARG;
  }

  env->CallVoidMethod(stream, method_seek, actual_offset);
  RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);

  if (newPosition != nullptr) {
    *newPosition = static_cast<UInt64>(actual_offset);
  }

  return S_OK;
}

HRESULT SeekableInputStream::GetSize(UInt64* size) {
  JavaEnv env;
  if (!env.IsValid()) return E_JAVA_EXCEPTION;

  jlong store_size = env->CallLongMethod(stream, method_size);
  RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);

  if (size != nullptr) {
    *size = static_cast<UInt64>(store_size);
  }

  return S_OK;
}

HRESULT SeekableInputStream::Initialize(JNIEnv* env) {
  if (initialized) {
    return S_OK;
  }

  jclass clazz = env->FindClass("com/hippo/a7zip/SeekableInputStream");
  if (clazz == nullptr) return E_CLASS_NOT_FOUND;

  method_read = env->GetMethodID(clazz, "read", "([BII)I");
  if (method_read == nullptr) return E_METHOD_NOT_FOUND;
  method_seek = env->GetMethodID(clazz, "seek", "(J)V");
  if (method_seek == nullptr) return E_METHOD_NOT_FOUND;
  method_tell = env->GetMethodID(clazz, "tell", "()J");
  if (method_tell == nullptr) return E_METHOD_NOT_FOUND;
  method_size = env->GetMethodID(clazz, "size", "()J");
  if (method_size == nullptr) return E_METHOD_NOT_FOUND;
  method_close = env->GetMethodID(clazz, "close", "()V");
  if (method_close == nullptr) return E_METHOD_NOT_FOUND;

  initialized = true;
  return JNI_OK;
}

HRESULT SeekableInputStream::Create(
    JNIEnv* env,
    jobject stream,
    CMyComPtr<IInStream>& in_stream
) {
  if (!initialized) {
    return E_NOT_INITIALIZED;
  }

  jobject g_stream = env->NewGlobalRef(stream);
  if (g_stream == nullptr) {
    return E_OUTOFMEMORY;
  }

  jbyteArray array = env->NewByteArray(ARRAY_SIZE);
  if (array == nullptr) {
    env->DeleteGlobalRef(g_stream);
    return E_FAILED_CONSTRUCT;
  }

  jbyteArray g_array = static_cast<jbyteArray>(env->NewGlobalRef(array));
  if (g_array == nullptr) {
    env->DeleteGlobalRef(g_stream);
    return E_OUTOFMEMORY;
  }

  in_stream = new SeekableInputStream(g_stream, g_array);

  return S_OK;
}
