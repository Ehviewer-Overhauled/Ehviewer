/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <archive.h>
#include <lzma.h>
#include <zlib.h>
#include "image/libjpeg-turbo/jconfigint.h"

#include "java_wrapper.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if ((*vm)->GetEnv(vm, (void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (!image_onLoad(vm)) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_hippo_Native_getlibarchiveVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, ARCHIVE_VERSION_STRING);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_Native_getliblzmaVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, lzma_version_string());
}

JNIEXPORT jstring JNICALL
Java_com_hippo_Native_getlibjpeg_1turboVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, VERSION);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_Native_getzlibVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, ZLIB_VERSION);
}
