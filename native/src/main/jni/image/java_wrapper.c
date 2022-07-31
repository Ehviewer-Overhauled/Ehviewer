/*
 * Copyright 2015 Hippo Seven
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

#include <stdlib.h>
#include <android/bitmap.h>
#include <GLES2/gl2.h>
#include <jni.h>

#include "java_wrapper.h"
#include "image.h"
#include "log.h"

static void *tile_buffer = NULL;

static int jniGetFDFromFileDescriptor(JNIEnv *env, jobject fileDescriptor) {
    jint fd = -1;
    jclass fdClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    if (fdClass != NULL) {
        jfieldID fdClassDescriptorFieldID = (*env)->GetFieldID(env, fdClass, "descriptor", "I");
        if (fdClassDescriptorFieldID != NULL && fileDescriptor != NULL) {
            fd = (*env)->GetIntField(env, fileDescriptor, fdClassDescriptorFieldID);
        }
    }

    return fd;
}

jobject create_image_object(JNIEnv *env, void *ptr, int format, int width, int height) {
    jclass image_clazz;
    jmethodID constructor;

    image_clazz = (*env)->FindClass(env, "com/hippo/image/Image");
    constructor = (*env)->GetMethodID(env, image_clazz, "<init>", "(JIII)V");
    return (*env)->NewObject(env, image_clazz, constructor,
                             (jlong) (uintptr_t) ptr, (jint) format, (jint) width,
                             (jint) height);
}

JNIEXPORT jobject JNICALL
Java_com_hippo_image_Image_nativeDecodeFdInt(JNIEnv *env, jclass clazz, jint fd) {
    IMAGE *image = createFromFd(fd);
    if (!image)
        return NULL;
    return create_image_object(env, image, image->isAnimated, image->width, image->height);
}

JNIEXPORT jobject JNICALL
Java_com_hippo_image_Image_nativeDecode(JNIEnv *env, jclass clazz, jobject fd) {
    return Java_com_hippo_image_Image_nativeDecodeFdInt(env, clazz,
                                                        jniGetFDFromFileDescriptor(env, fd));
}

JNIEXPORT jobject JNICALL
Java_com_hippo_image_Image_nativeCreate(JNIEnv *env, jclass clazz, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = NULL;
    void *image = NULL;

    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    image = create(info.width, info.height, pixels);
    AndroidBitmap_unlockPixels(env, bitmap);
    return create_image_object(env, image, 0, info.width, info.height);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRender(JNIEnv *env, jclass clazz, jlong ptr, jint src_x,
                                        jint src_y, jobject dst, jint dst_x, jint dst_y, jint width,
                                        jint height) {
    AndroidBitmapInfo info;
    void *pixels = NULL;

    AndroidBitmap_getInfo(env, dst, &info);
    AndroidBitmap_lockPixels(env, dst, &pixels);

    render((IMAGE *) ptr, src_x, src_y, pixels, info.width, info.height, dst_x, dst_y, width,
           height);

    AndroidBitmap_unlockPixels(env, dst);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeTexImage(JNIEnv *env, jclass clazz, jlong ptr, jboolean init,
                                          jint src_x, jint src_y, jint width, jint height) {
    if (width * height > IMAGE_TILE_MAX_SIZE)
        return;

    render((IMAGE *) ptr, src_x, src_y, tile_buffer, width, height, 0, 0, width, height);

    if (init) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                     tile_buffer);
    } else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE,
                        tile_buffer);
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeAdvance(JNIEnv *env, jclass clazz, jlong ptr) {
    advance((IMAGE *) ptr);
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetDelay(JNIEnv *env, jclass clazz, jlong ptr) {
    return ((IMAGE *) ptr)->delay;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_image_Image_nativeIsOpaque(JNIEnv *env, jclass clazz, jlong ptr) {
    return ((IMAGE *) ptr)->alpha;
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRecycle(JNIEnv *env, jclass clazz, jlong ptr) {
    recycle((IMAGE *) ptr);
}

bool image_onLoad() {
    tile_buffer = malloc(IMAGE_TILE_MAX_SIZE * 4);
    return tile_buffer != NULL;
}

void image_onUnload() {
    free(tile_buffer);
    tile_buffer = NULL;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_image_Image_nativeDecodeAddr(JNIEnv *env, jclass clazz, jlong addr) {
    Memarea *memarea = (Memarea *) addr;
    IMAGE *image = createFromAddr(memarea->buffer, memarea->size);
    free(memarea);
    return create_image_object(env, image, image->isAnimated, image->width, image->height);
}
