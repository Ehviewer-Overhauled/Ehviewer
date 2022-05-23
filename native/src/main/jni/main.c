#include <jni.h>
#include <archive.h>
#include <lzma.h>
#include <zstd.h>
#include <zlib.h>
#include "image/libjpeg-turbo/jconfigint.h"

#include "java_wrapper.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env;

    if ((*vm)->GetEnv(vm, (void**) (&env), JNI_VERSION_1_6) != JNI_OK) {
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
Java_com_hippo_Native_getlibzstdVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, ZSTD_VERSION_STRING);
}

JNIEXPORT jstring JNICALL
Java_com_hippo_Native_getzlibVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, ZLIB_VERSION);
}
