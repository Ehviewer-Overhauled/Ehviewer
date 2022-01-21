#include <jni.h>

#include "genuine.h"
#include "java_wrapper.h"
#include "JavaInitA7Zip.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**) (&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (!checkGenuine(env)) {
        return JNI_ERR;
    }

    if (!image_onLoad(vm)) {
        return JNI_ERR;
    }
    if (!a7zip_onLoad(vm)) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
