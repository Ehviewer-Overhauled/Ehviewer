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

#ifndef __A7ZIP_JAVA_HELPER_H__
#define __A7ZIP_JAVA_HELPER_H__

#include <jni.h>

#include <Common/MyWindows.h>

#define CLASS_NAME_ILLEGAL_STATE_EXCEPTION "java/lang/IllegalStateException"
#define CLASS_NAME_IO_EXCEPTION "java/io/IOException"
#define CLASS_NAME_ARCHIVE_EXCEPTION "com/hippo/a7zip/ArchiveException"
#define CLASS_NAME_PASSWORD_EXCEPTION "com/hippo/a7zip/PasswordException"

#define THROW_EXCEPTION(ENV, EXCEPTION_NAME, ...)                               \
  do {                                                                          \
    a7zip::JavaHelper::ThrowException((ENV), (EXCEPTION_NAME), __VA_ARGS__);    \
    return;                                                                     \
  } while (0)

#define THROW_EXCEPTION_RET(ENV, RET, EXCEPTION_NAME, ...)                      \
  do {                                                                          \
    a7zip::JavaHelper::ThrowException((ENV), (EXCEPTION_NAME), __VA_ARGS__);    \
    return (RET);                                                               \
  } while (0)

#define THROW_IO_EXCEPTION(ENV, CODE)                                           \
  THROW_EXCEPTION(ENV, CLASS_NAME_IO_EXCEPTION, CODE)

#define THROW_IO_EXCEPTION_RET(ENV, RET, CODE)                                  \
  THROW_EXCEPTION_RET(ENV, RET, CLASS_NAME_IO_EXCEPTION, CODE)

#define THROW_ARCHIVE_EXCEPTION(ENV, CODE)                                      \
  do {                                                                          \
    if ((CODE) == E_NO_PASSWORD || (CODE) == E_WRONG_PASSWORD) {                \
      THROW_EXCEPTION(ENV, CLASS_NAME_PASSWORD_EXCEPTION, CODE);                \
    } else {                                                                    \
      THROW_EXCEPTION(ENV, CLASS_NAME_ARCHIVE_EXCEPTION, CODE);                 \
    }                                                                           \
  } while (0)

#define THROW_ARCHIVE_EXCEPTION_RET(ENV, RET, CODE)                             \
  do {                                                                          \
    if ((CODE) == E_NO_PASSWORD || (CODE) == E_WRONG_PASSWORD) {                \
      THROW_EXCEPTION_RET(ENV, RET, CLASS_NAME_PASSWORD_EXCEPTION, CODE);       \
    } else {                                                                    \
      THROW_EXCEPTION_RET(ENV, RET, CLASS_NAME_ARCHIVE_EXCEPTION, CODE);        \
    }                                                                           \
  } while (0)

#define CHECK_CLOSED(ENV, NATIVE_PTR)                                           \
  do {                                                                          \
    if ((NATIVE_PTR) == 0) {                                                    \
      THROW_EXCEPTION(ENV, CLASS_NAME_ILLEGAL_STATE_EXCEPTION, "It's closed");  \
    }                                                                           \
  } while (0)

#define CHECK_CLOSED_RET(ENV, RET, NATIVE_PTR)                                  \
  do {                                                                          \
    if ((NATIVE_PTR) == 0) {                                                    \
      THROW_EXCEPTION_RET(                                                      \
          ENV, RET, CLASS_NAME_ILLEGAL_STATE_EXCEPTION, "It's closed");         \
    }                                                                           \
  } while (0)

namespace a7zip {
namespace JavaHelper {

jint ThrowException(JNIEnv* env, const char* exception_name, const char* message, ...);
jint ThrowException(JNIEnv* env, const char* exception_name, HRESULT code);

}
}

#endif //__A7ZIP_JAVA_HELPER_H__
