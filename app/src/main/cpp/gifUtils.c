#include <string.h>
#include <jni.h>

#define GIF_HEADER_87A "GIF87a"
#define GIF_HEADER_89A "GIF89a"

JNIEXPORT jboolean JNICALL
Java_com_hippo_Native_isGif(JNIEnv *env, jobject thiz, jobject buffer) {
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    return !memcmp(addr, GIF_HEADER_87A, 6) || !memcmp(addr, GIF_HEADER_89A, 6);
}
