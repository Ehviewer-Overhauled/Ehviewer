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

#include <jni.h>

#include "SeekableInputStream.h"
#include "JavaEnv.h"
#include "JavaInArchive.h"
#include "JavaInitA7Zip.h"
#include "JavaSeekableInputStream.h"
#include "JavaInputStream.h"
#include "OpenVolumeCallback.h"
#include "OutputStream.h"
#include "SevenZip.h"
#include "Utils.h"

using namespace a7zip;

#ifdef __cplusplus
extern "C"
{
#endif
bool a7zip_onLoad(JavaVM *vm) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return false;
    }

    JavaEnv::Initialize(vm);
    RETURN_JNI_ERR_IF_NOT_ZERO(SeekableInputStream::Initialize(env));
    RETURN_JNI_ERR_IF_NOT_ZERO(JavaSeekableInputStream::Initialize(env));
    RETURN_JNI_ERR_IF_NOT_ZERO(JavaInputStream::Initialize(env));
    RETURN_JNI_ERR_IF_NOT_ZERO(OpenVolumeCallback::Initialize(env));
    RETURN_JNI_ERR_IF_NOT_ZERO(OutputStream::Initialize(env));
    RETURN_JNI_ERR_IF_NOT_ZERO(SevenZip::Initialize());

    RETURN_JNI_ERR_IF_NOT_ZERO(JavaInArchive::RegisterMethods(static_cast<JNIEnv *>(env)));
    RETURN_JNI_ERR_IF_NOT_ZERO(
            JavaSeekableInputStream::RegisterMethods(static_cast<JNIEnv *>(env)));
    RETURN_JNI_ERR_IF_NOT_ZERO(JavaInputStream::RegisterMethods(static_cast<JNIEnv *>(env)));

    return true;
}
#ifdef __cplusplus
}
#endif
