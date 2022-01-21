//
// Created by Thom on 2019/3/20.
//

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <sys/system_properties.h>
#include "common.h"
#include "plt.h"

static volatile int mGenuine;

static bool onCheckTrue(JNIEnv *env __unused) {
#ifdef DEBUG_NATIVE
    has_native_libs();
#endif
#ifdef DEBUG_GENUINE_MOCK
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckFalse(JNIEnv *env __unused) {
#if defined(GENUINE_FALSE_CRASH)
    return false;
#elif defined(GENUINE_FALSE_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckFake(JNIEnv *env __unused) {
#if defined(GENUINE_FAKE_CRASH)
    return false;
#elif defined(GENUINE_FAKE_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckOverlay(JNIEnv *env __unused) {
#if defined(GENUINE_OVERLAY_CRASH)
    return false;
#elif defined(GENUINE_OVERLAY_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckOdex(JNIEnv *env __unused) {
#if defined(GENUINE_ODEX_CRASH)
    return false;
#elif defined(GENUINE_ODEX_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckDex(JNIEnv *env __unused) {
#if defined(GENUINE_DEX_CRASH)
    return false;
#elif defined(GENUINE_DEX_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckProxy(JNIEnv *env __unused) {
#if defined(GENUINE_PROXY_CRASH)
    return false;
#elif defined(GENUINE_PROXY_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckError(JNIEnv *env __unused) {
#if defined(GENUINE_ERROR_CRASH)
    return false;
#elif defined(GENUINE_ERROR_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckFatal(JNIEnv *env __unused) {
#if defined(GENUINE_FATAL_CRASH)
    return false;
#elif defined(GENUINE_FATAL_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

static bool onCheckNoapk(JNIEnv *env __unused) {
#if defined(GENUINE_NOAPK_CRASH)
    return false;
#elif defined(GENUINE_NOAPK_NATIVE)
    start_native_activity_async(env);
#endif
    return true;
}

bool setGenuine(JNIEnv *env, int genuine) {
    mGenuine = genuine;
    switch (genuine) {
        case CHECK_TRUE:
            return onCheckTrue(env);
        case CHECK_FALSE:
            return onCheckFalse(env);
        case CHECK_FAKE:
            return onCheckFake(env);
        case CHECK_OVERLAY:
            return onCheckOverlay(env);
        case CHECK_ODEX:
            return onCheckOdex(env);
        case CHECK_DEX:
            return onCheckDex(env);
        case CHECK_PROXY:
            return onCheckProxy(env);
        case CHECK_ERROR:
            return onCheckError(env);
        case CHECK_FATAL:
            return onCheckFatal(env);
        case CHECK_NOAPK:
            return onCheckNoapk(env);
        default:
            return true;
    }
}

int getGenuine() {
    return mGenuine;
}

char *getGenuinePackageName() {
#ifdef GET_GENUINE_PACKAGE_NAME
    return GET_GENUINE_PACKAGE_NAME();
#elif defined(GENUINE_NAME)
    static unsigned int m = 0;
    if (m == 0) {
        m = 20;
    } else if (m == 23) {
        m = 29;
    }
    char name[] = GENUINE_NAME;
    unsigned int length = sizeof(name) - 1;
    for (unsigned int i = 0; i < length; ++i) {
        name[i] ^= ((i + length) % m);
    }
    name[length] = '\0';
    return strdup(name);
#else
    return NULL;
#endif
}

__attribute__((__format__ (__printf__, 2, 0)))
void genuine_log_print(int prio, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    __android_log_vprint(prio, TAG, fmt, ap);
    va_end(ap);
}

static inline void fill_ro_build_version_sdk(char v[]) {
    // ro.build.version.sdk
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 's';
    v[0x1] = 'm';
    v[0x2] = '-';
    v[0x3] = 'f';
    v[0x4] = 'p';
    v[0x5] = 'o';
    v[0x6] = 'k';
    v[0x7] = 'l';
    v[0x8] = '\'';
    v[0x9] = '|';
    v[0xa] = 'n';
    v[0xb] = '~';
    v[0xc] = '~';
    v[0xd] = 'g';
    v[0xe] = '`';
    v[0xf] = '~';
    v[0x10] = '?';
    v[0x11] = 'a';
    v[0x12] = 'd';
    v[0x13] = 'j';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

int getSdk() {
    static int sdk = 0;
    if (sdk == 0) {
        char v1[0x20];
        char prop[PROP_VALUE_MAX] = {0};
        fill_ro_build_version_sdk(v1);
        __system_property_get(v1, prop);
        sdk = (int) strtol(prop, NULL, 10);
    }
    return sdk;
}

#ifdef DEBUG

void debug(JNIEnv *env, const char *format, jobject object) {
    if (object == NULL) {
        LOGI(format, NULL);
    } else {
        jclass objectClass = (*env)->FindClass(env, "java/lang/Object");
        jmethodID toString = (*env)->GetMethodID(env, objectClass, "toString",
                                                 "()Ljava/lang/String;");
        jstring string = (jstring) (*env)->CallObjectMethod(env, object, toString);
        const char *value = (*env)->GetStringUTFChars(env, string, NULL);
        LOGI(format, value);
        (*env)->ReleaseStringUTFChars(env, string, value);
        (*env)->DeleteLocalRef(env, string);
        (*env)->DeleteLocalRef(env, objectClass);
    }
}

#endif
