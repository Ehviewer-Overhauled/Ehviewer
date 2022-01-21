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

#include "JavaEnv.h"

using namespace a7zip;

JavaVM* JavaEnv::jvm = nullptr;

void JavaEnv::Initialize(JavaVM* jvm) {
  JavaEnv::jvm = jvm;
}

JavaEnv::JavaEnv() {
  env = nullptr;
  if (jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
    should_detach = jvm->AttachCurrentThread(&env, nullptr) == JNI_OK;
  } else {
    should_detach = false;
  }
}

JavaEnv::~JavaEnv() {
  if (jvm != nullptr && should_detach) {
    jvm->DetachCurrentThread();
    env = nullptr;
    should_detach = false;
  }
}

bool JavaEnv::IsValid() {
  return env != nullptr;
}
