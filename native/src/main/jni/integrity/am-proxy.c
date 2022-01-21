//
// Created by Thom on 2019/3/15.
//

#include "common.h"
#include "am-proxy.h"

static inline void fill_android_app_ActivityManager(char v[]) {
    // android/app/ActivityManager
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'e';
    v[0x1] = 'k';
    v[0x2] = 'b';
    v[0x3] = 'u';
    v[0x4] = 'g';
    v[0x5] = '`';
    v[0x6] = 'n';
    v[0x7] = '$';
    v[0x8] = 'm';
    v[0x9] = '}';
    v[0xa] = '~';
    v[0xb] = ' ';
    v[0xc] = 'Q';
    v[0xd] = 'r';
    v[0xe] = 'f';
    v[0xf] = 'z';
    v[0x10] = 'b';
    v[0x11] = '|';
    v[0x12] = 'b';
    v[0x13] = 'y';
    v[0x14] = 'L';
    v[0x15] = 'c';
    v[0x16] = 'm';
    v[0x17] = 'e';
    v[0x18] = 'b';
    v[0x19] = 'c';
    v[0x1a] = 'u';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static inline void fill_getService(char v[]) {
    // getService
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'd';
    v[0x1] = 'a';
    v[0x2] = 'q';
    v[0x3] = 'U';
    v[0x4] = 'e';
    v[0x5] = 's';
    v[0x6] = 't';
    v[0x7] = 'j';
    v[0x8] = 'g';
    v[0x9] = '`';
    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    v[0xa] = '\0';
}

static inline void fill_getService_signature(char v[]) {
    // ()Landroid/app/IActivityManager;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'e';
    v[0x4] = 'k';
    v[0x5] = 'b';
    v[0x6] = 'u';
    v[0x7] = 'g';
    v[0x8] = '`';
    v[0x9] = 'n';
    v[0xa] = '$';
    v[0xb] = 'm';
    v[0xc] = '}';
    v[0xd] = '~';
    v[0xe] = ' ';
    v[0xf] = 'Y';
    v[0x10] = 'P';
    v[0x11] = 'q';
    v[0x12] = 'g';
    v[0x13] = '}';
    v[0x14] = 'c';
    v[0x15] = '\x7f';
    v[0x16] = 'c';
    v[0x17] = 'a';
    v[0x18] = 'T';
    v[0x19] = '{';
    v[0x1a] = 'u';
    v[0x1b] = '}';
    v[0x1c] = 'z';
    v[0x1d] = '{';
    v[0x1e] = 'r';
    v[0x1f] = ':';
    for (unsigned int i = 0; i < 0x20; ++i) {
        v[i] ^= ((i + 0x20) % m);
    }
    v[0x20] = '\0';
}

static inline void fill_android_app_ActivityManagerNative(char v[]) {
    // android/app/ActivityManagerNative
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'c';
    v[0x1] = 'm';
    v[0x2] = '`';
    v[0x3] = 'w';
    v[0x4] = 'i';
    v[0x5] = 'n';
    v[0x6] = 'l';
    v[0x7] = '&';
    v[0x8] = 'k';
    v[0x9] = '{';
    v[0xa] = '|';
    v[0xb] = '"';
    v[0xc] = 'O';
    v[0xd] = 'l';
    v[0xe] = 'd';
    v[0xf] = 'x';
    v[0x10] = 'd';
    v[0x11] = 'z';
    v[0x12] = '`';
    v[0x13] = 'l';
    v[0x14] = '[';
    v[0x15] = 'v';
    v[0x16] = 'v';
    v[0x17] = 'x';
    v[0x18] = '}';
    v[0x19] = '~';
    v[0x1a] = 'n';
    v[0x1b] = 'S';
    v[0x1c] = '\x7f';
    v[0x1d] = 't';
    v[0x1e] = 'h';
    v[0x1f] = 't';
    v[0x20] = 'f';
    for (unsigned int i = 0; i < 0x21; ++i) {
        v[i] ^= ((i + 0x21) % m);
    }
    v[0x21] = '\0';
}

static inline void fill_getDefault(char v[]) {
    // getDefault
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'd';
    v[0x1] = 'a';
    v[0x2] = 'q';
    v[0x3] = 'B';
    v[0x4] = 'e';
    v[0x5] = 'g';
    v[0x6] = 'c';
    v[0x7] = 'v';
    v[0x8] = 'h';
    v[0x9] = 'q';
    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    v[0xa] = '\0';
}

static inline void fill_getDefault_signature(char v[]) {
    // ()Landroid/app/IActivityManager;
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'e';
    v[0x4] = 'k';
    v[0x5] = 'b';
    v[0x6] = 'u';
    v[0x7] = 'g';
    v[0x8] = '`';
    v[0x9] = 'n';
    v[0xa] = '$';
    v[0xb] = 'm';
    v[0xc] = '}';
    v[0xd] = '~';
    v[0xe] = ' ';
    v[0xf] = 'Y';
    v[0x10] = 'P';
    v[0x11] = 'q';
    v[0x12] = 'g';
    v[0x13] = '}';
    v[0x14] = 'c';
    v[0x15] = '\x7f';
    v[0x16] = 'c';
    v[0x17] = 'a';
    v[0x18] = 'T';
    v[0x19] = '{';
    v[0x1a] = 'u';
    v[0x1b] = '}';
    v[0x1c] = 'z';
    v[0x1d] = '{';
    v[0x1e] = 'r';
    v[0x1f] = ':';
    for (unsigned int i = 0; i < 0x20; ++i) {
        v[i] ^= ((i + 0x20) % m);
    }
    v[0x20] = '\0';
}

static inline void fill_android_os_BinderProxy(char v[]) {
    // android/os/BinderProxy
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'b';
    v[0x1] = 'j';
    v[0x2] = 'a';
    v[0x3] = 't';
    v[0x4] = 'h';
    v[0x5] = 'a';
    v[0x6] = 'm';
    v[0x7] = '%';
    v[0x8] = 'd';
    v[0x9] = '\x7f';
    v[0xa] = '"';
    v[0xb] = 'L';
    v[0xc] = 'f';
    v[0xd] = '~';
    v[0xe] = 'u';
    v[0xf] = 'w';
    v[0x10] = 'r';
    v[0x11] = 'Q';
    v[0x12] = 'p';
    v[0x13] = 'l';
    v[0x14] = '|';
    v[0x15] = '|';
    for (unsigned int i = 0; i < 0x16; ++i) {
        v[i] ^= ((i + 0x16) % m);
    }
    v[0x16] = '\0';
}

static inline void fill_android_os_IInterface(char v[]) {
    // android/os/IInterface
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'c';
    v[0x1] = 'm';
    v[0x2] = '`';
    v[0x3] = 'w';
    v[0x4] = 'i';
    v[0x5] = 'n';
    v[0x6] = 'l';
    v[0x7] = '&';
    v[0x8] = 'e';
    v[0x9] = 'x';
    v[0xa] = '#';
    v[0xb] = 'D';
    v[0xc] = 'G';
    v[0xd] = 'a';
    v[0xe] = 'd';
    v[0xf] = 't';
    v[0x10] = '`';
    v[0x11] = 'f';
    v[0x12] = '`';
    v[0x13] = 'a';
    v[0x14] = 'f';
    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    v[0x15] = '\0';
}

static inline void fill_asBinder(char v[]) {
    // asBinder
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = '`';
    v[0x1] = 'q';
    v[0x2] = 'A';
    v[0x3] = 'm';
    v[0x4] = 'k';
    v[0x5] = 'b';
    v[0x6] = 'e';
    v[0x7] = 's';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_asBinder_signature(char v[]) {
    // ()Landroid/os/IBinder;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = '+';
    v[0x1] = '-';
    v[0x2] = 'I';
    v[0x3] = 'g';
    v[0x4] = 'i';
    v[0x5] = 'l';
    v[0x6] = '{';
    v[0x7] = 'e';
    v[0x8] = 'b';
    v[0x9] = 'h';
    v[0xa] = '"';
    v[0xb] = 'a';
    v[0xc] = '|';
    v[0xd] = '?';
    v[0xe] = 'X';
    v[0xf] = 'P';
    v[0x10] = 'i';
    v[0x11] = 'o';
    v[0x12] = 'f';
    v[0x13] = 'f';
    v[0x14] = 'v';
    v[0x15] = '>';
    for (unsigned int i = 0; i < 0x16; ++i) {
        v[i] ^= ((i + 0x16) % m);
    }
    v[0x16] = '\0';
}

static inline void fill_android_os_ServiceManager(char v[]) {
    // android/os/ServiceManager
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'c';
    v[0x1] = 'm';
    v[0x2] = '`';
    v[0x3] = 'w';
    v[0x4] = 'i';
    v[0x5] = 'n';
    v[0x6] = 'l';
    v[0x7] = '&';
    v[0x8] = 'e';
    v[0x9] = 'x';
    v[0xa] = '#';
    v[0xb] = '^';
    v[0xc] = 'k';
    v[0xd] = '}';
    v[0xe] = 'f';
    v[0xf] = 'x';
    v[0x10] = 'q';
    v[0x11] = 'v';
    v[0x12] = 'Y';
    v[0x13] = 't';
    v[0x14] = 'x';
    v[0x15] = 'a';
    v[0x16] = 'f';
    v[0x17] = 'g';
    v[0x18] = 'q';
    for (unsigned int i = 0; i < 0x19; ++i) {
        v[i] ^= ((i + 0x19) % m);
    }
    v[0x19] = '\0';
}

static inline void fill_getService_signature_IBinder(char v[]) {
    // (Ljava/lang/String;)Landroid/os/IBinder;
    static unsigned int m = 0;

    if (m == 0) {
        m = 37;
    } else if (m == 41) {
        m = 43;
    }

    v[0x0] = '+';
    v[0x1] = 'H';
    v[0x2] = 'o';
    v[0x3] = 'g';
    v[0x4] = 'q';
    v[0x5] = 'i';
    v[0x6] = '&';
    v[0x7] = 'f';
    v[0x8] = 'j';
    v[0x9] = 'b';
    v[0xa] = 'j';
    v[0xb] = '!';
    v[0xc] = '\\';
    v[0xd] = 'd';
    v[0xe] = 'c';
    v[0xf] = '{';
    v[0x10] = '}';
    v[0x11] = 's';
    v[0x12] = '.';
    v[0x13] = '?';
    v[0x14] = '[';
    v[0x15] = 'y';
    v[0x16] = 'w';
    v[0x17] = '~';
    v[0x18] = 'i';
    v[0x19] = 's';
    v[0x1a] = 't';
    v[0x1b] = 'z';
    v[0x1c] = '0';
    v[0x1d] = 'O';
    v[0x1e] = 'R';
    v[0x1f] = '\r';
    v[0x20] = 'j';
    v[0x21] = 'f';
    v[0x22] = 'i';
    v[0x23] = 'o';
    v[0x24] = 'f';
    v[0x25] = 'f';
    v[0x26] = 'v';
    v[0x27] = '>';
    for (unsigned int i = 0; i < 0x28; ++i) {
        v[i] ^= ((i + 0x28) % m);
    }
    v[0x28] = '\0';
}

static inline void fill_activity(char v[]) {
    // activity
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = '`';
    v[0x1] = 'a';
    v[0x2] = 'w';
    v[0x3] = 'm';
    v[0x4] = 's';
    v[0x5] = 'o';
    v[0x6] = 't';
    v[0x7] = 'x';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_getName(char v[]) {
    // getName
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'e';
    v[0x1] = 'f';
    v[0x2] = 'p';
    v[0x3] = 'N';
    v[0x4] = '`';
    v[0x5] = 'o';
    v[0x6] = 'f';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_getName_signature(char v[]) {
    // ()Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = ')';
    v[0x1] = '+';
    v[0x2] = 'O';
    v[0x3] = 'n';
    v[0x4] = 'd';
    v[0x5] = 'p';
    v[0x6] = 'f';
    v[0x7] = '\'';
    v[0x8] = 'e';
    v[0x9] = 'k';
    v[0xa] = 'e';
    v[0xb] = 'k';
    v[0xc] = '"';
    v[0xd] = ']';
    v[0xe] = '{';
    v[0xf] = 'b';
    v[0x10] = 'x';
    v[0x11] = '|';
    v[0x12] = 'g';
    v[0x13] = ':';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_activity_manager_service_is_s(char v[]) {
    // activity manager service is %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 29;
    } else if (m == 31) {
        m = 37;
    }

    v[0x0] = '`';
    v[0x1] = 'a';
    v[0x2] = 'w';
    v[0x3] = 'm';
    v[0x4] = 's';
    v[0x5] = 'o';
    v[0x6] = 's';
    v[0x7] = 'q';
    v[0x8] = ')';
    v[0x9] = 'g';
    v[0xa] = 'j';
    v[0xb] = 'b';
    v[0xc] = 'l';
    v[0xd] = 'i';
    v[0xe] = 'j';
    v[0xf] = 'b';
    v[0x10] = '1';
    v[0x11] = 'a';
    v[0x12] = 'v';
    v[0x13] = 'f';
    v[0x14] = 'c';
    v[0x15] = '\x7f';
    v[0x16] = 't';
    v[0x17] = '}';
    v[0x18] = '9';
    v[0x19] = 's';
    v[0x1a] = 'h';
    v[0x1b] = '<';
    v[0x1c] = '%';
    v[0x1d] = 'r';
    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    v[0x1e] = '\0';
}

static inline void showError(JNIEnv *env, jclass object) {
    char v1[0x8], v2[0x1f];

    if (object == NULL) {
        fill_activity_manager_service_is_s(v2); // 0x1f
        LOGE(v2, NULL);
    } else {
        fill_getName(v1); // 0x8
        fill_getName_signature(v2); // 0x15

        jclass clazz = (*env)->GetObjectClass(env, object);
        jmethodID method = (*env)->GetMethodID(env, clazz, v1, v2);
        jstring string = (*env)->CallObjectMethod(env, object, method);

        const char *name = (*env)->GetStringUTFChars(env, string, NULL);
        fill_activity_manager_service_is_s(v2); // 0x1f
        LOGE(v2, name);

        (*env)->ReleaseStringUTFChars(env, string, name);
        (*env)->DeleteLocalRef(env, string);
        (*env)->DeleteLocalRef(env, clazz);
    }
}

bool isAmProxy(JNIEnv *env, int sdk) {
    char v1[0xb], v2[0x29];
    bool proxy = false;
    jmethodID method;
    jclass classActivityManager;

    if (sdk >= 26) {
        fill_android_app_ActivityManager(v2); // 0x1b + 1
        classActivityManager = (*env)->FindClass(env, v2);
        debug(env, "ActivityManager: %s", classActivityManager);
        fill_getService(v1); // 0xa + 1
        fill_getService_signature(v2); // 0x20 + 1
        method = (*env)->GetStaticMethodID(env, classActivityManager, v1, v2);
#ifdef DEBUG
        LOGI("ActivityManager.getService: %p", method);
#endif
        if (method == NULL) {
#ifdef DEBUG
            LOGW("cannot find ActivityManager.getService");
#endif
            (*env)->ExceptionClear(env);
            goto clean3;
        }
    } else {
        fill_android_app_ActivityManagerNative(v2); // 0x21 + 1
        classActivityManager = (*env)->FindClass(env, v2);
        debug(env, "ActivityManagerNative: %s", classActivityManager);
        fill_getDefault(v1); // 0xa + 1
        fill_getDefault_signature(v2); // 0x20 + 1
        method = (*env)->GetStaticMethodID(env, classActivityManager, v1, v2);
#ifdef DEBUG
        LOGI("ActivityManagerNative.getDefault: %p", method);
#endif
    }

    jobject activityManager = (*env)->CallStaticObjectMethod(env, classActivityManager, method);
    if (activityManager == NULL) {
#ifdef DEBUG
        LOGW("activity manager is null");
#endif
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        goto clean3;
    }
    debug(env, "activity manager: %s", activityManager);

    jclass binderClass = NULL;
    jobject binder = NULL;

    fill_android_os_BinderProxy(v2); // 0x16 + 1
    jclass classBinderProxy = (*env)->FindClass(env, v2);
    if (classBinderProxy == NULL) {
#ifdef DEBUG
        LOGW("cannot find BinderProxy");
#endif
        (*env)->ExceptionClear(env);
    } else {
        debug(env, "BinderProxy: %s", classBinderProxy);
    }

    fill_android_os_IInterface(v2); // 0x15 + 1
    jclass classIInterface = (*env)->FindClass(env, v2);
    debug(env, "IInterface: %s", classIInterface);

    if (!(*env)->IsInstanceOf(env, activityManager, classIInterface)) {
#ifdef DEBUG
        LOGW("activity manager is not IInterface");
#endif
        goto clean2;
    }

    fill_asBinder(v1); // 0x8 + 1
    fill_asBinder_signature(v2); // 0x16 + 1
    method = (*env)->GetMethodID(env, classIInterface, v1, v2);
#ifdef DEBUG
    LOGI("IInterface.asBinder: %p", method);
#endif
    binder = (*env)->CallObjectMethod(env, activityManager, method);
    if (binder == NULL) {
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        showError(env, NULL);
        proxy = true;
        goto clean2;
    }
    debug(env, "binder: %s", binder);

    binderClass = (*env)->GetObjectClass(env, binder);
    debug(env, "binder class: %s", binderClass);

    if (classBinderProxy != NULL && !(*env)->IsSameObject(env, binderClass, classBinderProxy)) {
        showError(env, binderClass);
        proxy = true;
        goto clean2;
    }

    fill_android_os_ServiceManager(v2); // 0x19 + 1
    jclass classServiceManager = (*env)->FindClass(env, v2);
    if (classServiceManager == NULL) {
#ifdef DEBUG
        LOGW("cannot find ServiceManager");
#endif
        (*env)->ExceptionClear(env);
        goto clean2;
    }
    debug(env, "ServiceManager: %s", classServiceManager);

    fill_getService(v1); // 0xa + 1
    fill_getService_signature_IBinder(v2); // 0x28 + 1
    method = (*env)->GetStaticMethodID(env, classServiceManager, v1, v2);

#ifdef DEBUG
    LOGI("ServiceManager.getService: %p", method);
#endif
    if (method == NULL) {
        (*env)->ExceptionClear(env);
        goto clean1;
    }

    fill_activity(v2); // 0x8 + 1
    jstring stringActivity = (*env)->NewStringUTF(env, v2);
    jobject service = (*env)->CallStaticObjectMethod(env, classServiceManager, method,
                                                     stringActivity);
    jclass serviceClass = NULL;

    if (service == NULL) {
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        showError(env, NULL);
        proxy = true;
        goto clean;
    }
    debug(env, "service: %s", service);

    serviceClass = (*env)->GetObjectClass(env, service);
    debug(env, "service class: %s", serviceClass);
    if (classBinderProxy != NULL && !(*env)->IsSameObject(env, serviceClass, classBinderProxy)) {
        showError(env, serviceClass);
        proxy = true;
        goto clean;
    }

    bool equals = (*env)->IsSameObject(env, binder, service);
#ifdef DEBUG
    LOGI("binder: %p, service: %p, equals: %d", binder, service, equals);
#endif
    proxy = !equals;

clean:
    if (serviceClass != NULL) {
        (*env)->DeleteLocalRef(env, serviceClass);
    }
    if (service != NULL) {
        (*env)->DeleteLocalRef(env, service);
    }
    (*env)->DeleteLocalRef(env, stringActivity);

clean1:
    (*env)->DeleteLocalRef(env, classServiceManager);

clean2:
    if (binderClass != NULL) {
        (*env)->DeleteLocalRef(env, binderClass);
    }
    if (binder != NULL) {
        (*env)->DeleteLocalRef(env, binder);
    }
    if (classBinderProxy != NULL) {
        (*env)->DeleteLocalRef(env, classBinderProxy);
    }
    (*env)->DeleteLocalRef(env, classIInterface);
    (*env)->DeleteLocalRef(env, activityManager);

clean3:
    (*env)->DeleteLocalRef(env, classActivityManager);

    return proxy;
}
