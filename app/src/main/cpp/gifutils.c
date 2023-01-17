#include <jni.h>

#include <android/log.h>

#define LOG_TAG "gifUtils"

#include "ehviewer.h"

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_rewriteGifSource(JNIEnv *env, jclass clazz, jobject buffer) {
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    size_t size = (*env)->GetDirectBufferCapacity(env, buffer);
    EH_UNUSED(addr);
    EH_UNUSED(size);
}
