#include <jni.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/sendfile.h>

#include <android/log.h>
#include <string.h>
#include <errno.h>

#define LOG_TAG "mmap_utils"

#include "ehviewer.h"

// TODO: Replace it with AFileDescriptor_getFd when minsdk 31
// https://developer.android.com/ndk/reference/group/file-descriptor
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
Java_com_hippo_Native_sendfile(JNIEnv *env, jclass clazz, jint from, jint to) {
    struct stat st;
    fstat(from, &st);
    sendfile(to, from, 0, st.st_size);
}
