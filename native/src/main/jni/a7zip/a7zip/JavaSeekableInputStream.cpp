/*
 * Copyright 2018 Hippo Seven
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

#include "JavaSeekableInputStream.h"

#include <type_traits>
#include <Common/MyCom.h>

#include "JavaHelper.h"
#include "Utils.h"

using namespace a7zip;

static void NativeSeek(
    JNIEnv* env,
    jclass,
    jlong native_ptr,
    jlong pos
) {
  CHECK_CLOSED(env, native_ptr);
  IInStream* stream = reinterpret_cast<IInStream*>(native_ptr);

  UInt64 newPosition; // Ignored
  HRESULT result = stream->Seek(pos, STREAM_SEEK_SET, &newPosition);
  if (result != S_OK) THROW_IO_EXCEPTION(env, result);
}

static jlong NativeTell(
    JNIEnv* env,
    jclass,
    jlong native_ptr
) {
  CHECK_CLOSED_RET(env, 0, native_ptr);
  IInStream* stream = reinterpret_cast<IInStream*>(native_ptr);

  UInt64 newPosition;
  HRESULT result = stream->Seek(0, STREAM_SEEK_CUR, &newPosition);
  if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);
  return static_cast<jlong>(newPosition);
}

static jlong NativeSize(
    JNIEnv* env,
    jclass,
    jlong native_ptr
) {
  CHECK_CLOSED_RET(env, 0, native_ptr);
  IInStream* stream = reinterpret_cast<IInStream*>(native_ptr);

  // Try to get IStreamGetSize
  CMyComPtr<IStreamGetSize> stream_get_size;
  stream->QueryInterface(IID_IStreamGetSize, reinterpret_cast<void **>(&stream_get_size));

  if (stream_get_size != nullptr) {
    // It's an IStreamGetSize
    UInt64 size;
    HRESULT result = stream_get_size->GetSize(&size);
    if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);
    return static_cast<jlong>(size);
  } else {
    // It's not an IStreamGetSize
    // Backup current position
    UInt64 cur;
    HRESULT result = stream->Seek(0, STREAM_SEEK_CUR, &cur);
    if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);

    // Seek to the end of steam to get size
    UInt64 size;
    result = stream->Seek(0, STREAM_SEEK_END, &size);
    if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);

    // Back to previous position
    UInt64 ignored;
    result = stream->Seek(cur, STREAM_SEEK_SET, &ignored);
    if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);

    return static_cast<jlong>(size);
  }
}

static jint NativeRead(
    JNIEnv* env,
    jclass,
    jlong native_ptr,
    jbyteArray array,
    jint offset,
    jint length
) {
  CHECK_CLOSED_RET(env, 0, native_ptr);
  IInStream* stream = reinterpret_cast<IInStream*>(native_ptr);

  jbyte* bytes = env->GetByteArrayElements(array, nullptr);
  if (bytes == nullptr) THROW_IO_EXCEPTION_RET(env, 0, E_JAVA_EXCEPTION);

  UInt32 processed_size;
  HRESULT result = stream->Read(bytes + offset, static_cast<UInt32>(length), &processed_size);
  env->ReleaseByteArrayElements(array, bytes, 0);
  if (result != S_OK) THROW_IO_EXCEPTION_RET(env, 0, result);

  if (length != 0 && processed_size == 0) {
    // EOF
    return -1;
  }

  return processed_size;
}

static void NativeClose(
    JNIEnv* env,
    jclass,
    jlong native_ptr
) {
  CHECK_CLOSED(env, native_ptr);
  IInStream* stream = reinterpret_cast<IInStream*>(native_ptr);
  stream->Release();
}

static bool initialized = false;
static jclass class_native_in_stream = nullptr;
static jmethodID constructor_native_in_stream = nullptr;

HRESULT JavaSeekableInputStream::Initialize(JNIEnv* env) {
  if (initialized) {
    return S_OK;
  }

  class_native_in_stream = env->FindClass("com/hippo/a7zip/NativeSeekableInputStream");
  if (class_native_in_stream == nullptr) return E_CLASS_NOT_FOUND;
  class_native_in_stream = static_cast<jclass>(env->NewGlobalRef(class_native_in_stream));
  if (class_native_in_stream == nullptr) return E_OUTOFMEMORY;

  constructor_native_in_stream = env->GetMethodID(class_native_in_stream, "<init>", "(J)V");
  if (constructor_native_in_stream == nullptr) return E_METHOD_NOT_FOUND;

  initialized = true;
  return JNI_OK;
}

static JNINativeMethod stream_methods[] = {
    { "nativeSeek",
      "(JJ)V",
      reinterpret_cast<void *>(NativeSeek) },
    { "nativeTell",
      "(J)J",
      reinterpret_cast<void *>(NativeTell) },
    { "nativeSize",
      "(J)J",
      reinterpret_cast<void *>(NativeSize) },
    { "nativeRead",
      "(J[BII)I",
      reinterpret_cast<void *>(NativeRead) },
    { "nativeClose",
      "(J)V",
      reinterpret_cast<void *>(NativeClose) }
};

HRESULT JavaSeekableInputStream::RegisterMethods(JNIEnv* env) {
  if (!initialized) {
    return E_NOT_INITIALIZED;
  }

  jint result = env->RegisterNatives(class_native_in_stream, stream_methods, std::extent<decltype(stream_methods)>::value);
  if (result < 0) {
    return E_FAILED_REGISTER;
  }

  return S_OK;
}

HRESULT JavaSeekableInputStream::NewInstance(JNIEnv* env, IInStream* stream, jobject* object) {
  *object = nullptr;

  if (!initialized) {
    return E_NOT_INITIALIZED;
  }

  *object = env->NewObject(class_native_in_stream, constructor_native_in_stream, stream);

  if (*object == nullptr) {
    // Clear java exception and return
    RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(env);
    // No java exception, but it's still an error
    return E_UNKNOWN_ERROR;
  }

  // Ignore if any exception is pending
  CLEAR_IF_EXCEPTION_PENDING(env);
  return S_OK;
}
