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

#include "JavaHelper.h"

#include <jni.h>

#include <cstdio>

#include "Utils.h"

#define MAX_MSG_SIZE 1024

using namespace a7zip;

static jint ThrowExceptionInternal(
    JNIEnv* env,
    const char* exception_name,
    const char* message,
    va_list va_args
) {
  char formatted_message[MAX_MSG_SIZE];
  vsnprintf(formatted_message, MAX_MSG_SIZE, message, va_args);
  jclass exception_class = env->FindClass(exception_name);
  return env->ThrowNew(exception_class, formatted_message);
}

jint JavaHelper::ThrowException(
    JNIEnv* env,
    const char* exception_name,
    const char* message,
    ...
) {
  va_list va_args;
  va_start(va_args, message);
  jint result = ThrowExceptionInternal(env, exception_name, message, va_args);
  va_end(va_args);
  return result;
}

static const char* GetMessageForCode(HRESULT code) {
  switch (code) {
    case E_NOT_INITIALIZED:
      return "The module is not initialized";
    case S_OK:
    case E_INTERNAL:
      return "a7zip is buggy";
    case E_CLASS_NOT_FOUND:
      return "Can't find the class";
    case E_METHOD_NOT_FOUND:
      return "Can't find the method";
    case E_JAVA_EXCEPTION:
      return "Catch a java exception";
    case E_FAILED_CONSTRUCT:
      return "Failed to create new class";
    case E_FAILED_REGISTER:
      return "Failed to register methods";
    case E_INCONSISTENT_PROP_TYPE:
      return "Inconsistent property type";
    case E_EMPTY_PROP:
      return "Empty property";
    case E_UNKNOWN_FORMAT:
      return "Unknown archive format";
    case E_UNSUPPORTED_EXTRACT_MODE:
      return "Unsupported extract mode";
    case E_NO_OUT_STREAM:
      return "No out stream";
    case E_UNSUPPORTED_METHOD:
      return "Unsupported method";
    case E_DATA_ERROR:
      return "Data error";
    case E_DATA_ERROR_ENCRYPTED:
      return "Data Error in encrypted file. Wrong password?";
    case E_CRC_ERROR:
      return "CRC failed";
    case E_CRC_ERROR_ENCRYPTED:
      return "CRC Failed in encrypted file. Wrong password?";
    case E_UNAVAILABLE:
      return "Unavailable data";
    case E_UNEXPECTED_END:
      return "Unexpected end of data";
    case E_DATA_AFTER_END:
      return "There are some data after the end of the payload data";
    case E_IS_NOT_ARC:
      return "Is not archive";
    case E_HEADERS_ERROR:
      return "Headers Error";
    case E_WRONG_PASSWORD:
      return "Wrong password";
    case E_NO_PASSWORD:
      return "No password";
    case E_NOTIMPL:
      return "Not implemented";
    case E_OUTOFMEMORY:
      return "Out of memory";
    case E_UNKNOWN_ERROR:
    default:
      return "Unknown error.";
  }
}

jint JavaHelper::ThrowException(
    JNIEnv* env,
    const char* exception_name,
    HRESULT code
) {
  return ThrowException(env, exception_name, GetMessageForCode(code));
}
