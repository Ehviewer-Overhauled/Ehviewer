#include <jni.h>

// This JNI function makes https call and save responseBody directly to filesystem to avoid copy on JVM heap
JNIEXPORT jlong JNICALL
Java_com_hippo_ehviewer_spider_SpiderDenKt_makeHttpsCallAndSaveToFd(JNIEnv *env, jclass clazz,jstring url, jboolean do_sni, jint fd) {
    // TODO: implement makeHttpsCallAndSaveToFd()
}

// Is KTLS supported? if not, we maybe use [TLSChannel]https://github.com/marianobarrios/tls-channel and do DirectByteBuffer IO
JNIEXPORT jboolean JNICALL
Java_com_hippo_ehviewer_spider_SpiderDenKt_kernelSupportKTLS(JNIEnv *env, jclass clazz) {
    // TODO: implement kernelSupportKTLS()
}
