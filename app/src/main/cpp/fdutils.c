#include <jni.h>
#include <sys/mman.h>

#include <android/log.h>
#include <string.h>
#include <errno.h>

#define LOG_TAG "mmap_utils"

#include "ehviewer.h"

JNIEXPORT jint JNICALL
Java_com_hippo_Native_getFd(JNIEnv *env, jclass clazz, jobject fileDescriptor) {
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

JNIEXPORT void JNICALL
Java_com_hippo_Native_unmapDirectByteBuffer(JNIEnv *env, jclass clazz, jobject buffer) {
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    size_t size = (*env)->GetDirectBufferCapacity(env, buffer);
    if (munmap(addr, size)) LOGE("munmap addr:%p, size%zu failed!", addr, size);
}
