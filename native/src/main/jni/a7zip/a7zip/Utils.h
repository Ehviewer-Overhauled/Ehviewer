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

#ifndef __A7ZIP_UTILS_H__
#define __A7ZIP_UTILS_H__

#define DEFAULT_BUFFER_SIZE (4 * 1024)

#define MIN(a, b) (((a) < (b)) ? (a) : (b))
#define MAX(a, b) (((a) > (b)) ? (a) : (b))


#define E_NOT_INITIALIZED ((HRESULT)0x82210000L)
#define E_INTERNAL ((HRESULT)0x82210001L)
#define E_UNKNOWN_ERROR ((HRESULT)0x82210002L)

#define E_CLASS_NOT_FOUND ((HRESULT)0x82220000L)
#define E_METHOD_NOT_FOUND ((HRESULT)0x82220001L)
#define E_JAVA_EXCEPTION ((HRESULT)0x82220002L)
#define E_FAILED_CONSTRUCT ((HRESULT)0x82220003L)
#define E_FAILED_REGISTER ((HRESULT)0x82220004L)
#define E_FAILED_UNREGISTER ((HRESULT)0x82220005L)

#define E_INCONSISTENT_PROP_TYPE ((HRESULT)0x82240000L)
#define E_EMPTY_PROP ((HRESULT)0x82240001L)
#define E_UNKNOWN_FORMAT ((HRESULT)0x82240002L)
#define E_UNSUPPORTED_EXTRACT_MODE ((HRESULT)0x82240003L)
#define E_NO_OUT_STREAM ((HRESULT)0x82240004L)

#define E_UNSUPPORTED_METHOD ((HRESULT)0x82250000L)
#define E_DATA_ERROR ((HRESULT)0x82250001L)
#define E_DATA_ERROR_ENCRYPTED ((HRESULT)0x82250002L)
#define E_CRC_ERROR ((HRESULT)0x82250003L)
#define E_CRC_ERROR_ENCRYPTED ((HRESULT)0x82250004L)
#define E_UNAVAILABLE ((HRESULT)0x82250005L)
#define E_UNEXPECTED_END ((HRESULT)0x82250006L)
#define E_DATA_AFTER_END ((HRESULT)0x82250007L)
#define E_IS_NOT_ARC ((HRESULT)0x82250008L)
#define E_HEADERS_ERROR ((HRESULT)0x82250009L)
#define E_WRONG_PASSWORD ((HRESULT)0x82250010L)
#define E_NO_PASSWORD ((HRESULT)0x82250011L)

#define CLEAR_IF_EXCEPTION_PENDING(ENV)                               \
  do {                                                                \
    if ((ENV)->ExceptionCheck()) {                                    \
      (ENV)->ExceptionDescribe();                                     \
      (ENV)->ExceptionClear();                                        \
    }                                                                 \
  } while (0)

#define RETURN_E_JAVA_EXCEPTION_IF_EXCEPTION_PENDING(ENV)             \
  do {                                                                \
    if ((ENV)->ExceptionCheck()) {                                    \
      (ENV)->ExceptionDescribe();                                     \
      (ENV)->ExceptionClear();                                        \
      return E_JAVA_EXCEPTION;                                        \
    }                                                                 \
  } while (0)

#define RETURN_JNI_ERR_IF_NOT_ZERO(VALUE)                             \
  do {                                                                \
    if ((VALUE) != 0) {                                               \
      return JNI_ERR;                                                 \
    }                                                                 \
  } while (0)

#define RETURN_SAME_IF_NOT_ZERO(VALUE)                                \
  do {                                                                \
    auto __temp__ = (VALUE);                                          \
    if (__temp__ != 0) {                                              \
      return __temp__;                                                \
    }                                                                 \
  } while (0)

#define CONTINUE_IF_NOT_ZERO(VALUE)                                   \
  do {                                                                \
    if ((VALUE) != 0) {                                               \
      continue;                                                       \
    }                                                                 \
  } while (0)

#endif //__A7ZIP_UTILS_H__
