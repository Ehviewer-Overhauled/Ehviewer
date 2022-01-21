//
// Created by Thom on 2019/3/15.
//

#include <string.h>
#include <android/log.h>
#include "common.h"
#include "pm.h"

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

static inline void fill_package(char v[]) {
    // package
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'r';
    v[0x1] = 'b';
    v[0x2] = 'g';
    v[0x3] = 'k';
    v[0x4] = '`';
    v[0x5] = 'e';
    v[0x6] = 'f';
    for (unsigned int i = 0; i < 0x7; ++i) {
        v[i] ^= ((i + 0x7) % m);
    }
    v[0x7] = '\0';
}

static inline void fill_android_content_pm_IPackageManager$Stub(char v[]) {
    // android/content/pm/IPackageManager$Stub
    static unsigned int m = 0;

    if (m == 0) {
        m = 37;
    } else if (m == 41) {
        m = 43;
    }

    v[0x0] = 'c';
    v[0x1] = 'm';
    v[0x2] = '`';
    v[0x3] = 'w';
    v[0x4] = 'i';
    v[0x5] = 'n';
    v[0x6] = 'l';
    v[0x7] = '&';
    v[0x8] = 'i';
    v[0x9] = 'd';
    v[0xa] = 'b';
    v[0xb] = 'y';
    v[0xc] = 'k';
    v[0xd] = 'a';
    v[0xe] = 'd';
    v[0xf] = '>';
    v[0x10] = 'b';
    v[0x11] = '~';
    v[0x12] = ';';
    v[0x13] = '\\';
    v[0x14] = 'F';
    v[0x15] = 'v';
    v[0x16] = '{';
    v[0x17] = 'r';
    v[0x18] = '{';
    v[0x19] = '|';
    v[0x1a] = 'y';
    v[0x1b] = 'P';
    v[0x1c] = '\x7f';
    v[0x1d] = 'q';
    v[0x1e] = 'A';
    v[0x1f] = 'F';
    v[0x20] = 'G';
    v[0x21] = 'Q';
    v[0x22] = '\x00';
    v[0x23] = 'S';
    v[0x24] = 'u';
    v[0x25] = 'w';
    v[0x26] = 'a';
    for (unsigned int i = 0; i < 0x27; ++i) {
        v[i] ^= ((i + 0x27) % m);
    }
    v[0x27] = '\0';
}

static inline void fill_asInterface(char v[]) {
    // asInterface
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'e';
    v[0x1] = 'v';
    v[0x2] = 'O';
    v[0x3] = 'n';
    v[0x4] = 'u';
    v[0x5] = 'g';
    v[0x6] = 'q';
    v[0x7] = 'b';
    v[0x8] = 'd';
    v[0x9] = 'e';
    v[0xa] = 'e';
    for (unsigned int i = 0; i < 0xb; ++i) {
        v[i] ^= ((i + 0xb) % m);
    }
    v[0xb] = '\0';
}

static inline void fill_asInterface_signature(char v[]) {
    // (Landroid/os/IBinder;)Landroid/content/pm/IPackageManager;
    static unsigned int m = 0;

    if (m == 0) {
        m = 53;
    } else if (m == 59) {
        m = 61;
    }

    v[0x0] = '-';
    v[0x1] = 'J';
    v[0x2] = 'f';
    v[0x3] = 'f';
    v[0x4] = 'm';
    v[0x5] = 'x';
    v[0x6] = 'd';
    v[0x7] = 'e';
    v[0x8] = 'i';
    v[0x9] = '!';
    v[0xa] = '`';
    v[0xb] = 'c';
    v[0xc] = '>';
    v[0xd] = '[';
    v[0xe] = 'Q';
    v[0xf] = '}';
    v[0x10] = '{';
    v[0x11] = 'r';
    v[0x12] = 'r';
    v[0x13] = 'j';
    v[0x14] = '"';
    v[0x15] = '3';
    v[0x16] = 'W';
    v[0x17] = '}';
    v[0x18] = 's';
    v[0x19] = 'z';
    v[0x1a] = 'm';
    v[0x1b] = 'O';
    v[0x1c] = 'H';
    v[0x1d] = 'F';
    v[0x1e] = '\x0c';
    v[0x1f] = 'G';
    v[0x20] = 'J';
    v[0x21] = 'H';
    v[0x22] = 'S';
    v[0x23] = 'M';
    v[0x24] = 'G';
    v[0x25] = '^';
    v[0x26] = '\x04';
    v[0x27] = '\\';
    v[0x28] = '@';
    v[0x29] = '\x01';
    v[0x2a] = 'f';
    v[0x2b] = '`';
    v[0x2c] = 'P';
    v[0x2d] = 'Q';
    v[0x2e] = 'X';
    v[0x2f] = 'U';
    v[0x30] = 'g';
    v[0x31] = 'd';
    v[0x32] = 'O';
    v[0x33] = 'b';
    v[0x34] = 'j';
    v[0x35] = 'd';
    v[0x36] = 'a';
    v[0x37] = 'b';
    v[0x38] = 'z';
    v[0x39] = '2';
    for (unsigned int i = 0; i < 0x3a; ++i) {
        v[i] ^= ((i + 0x3a) % m);
    }
    v[0x3a] = '\0';
}

static inline void fill_getApplicationInfo(char v[]) {
    // getApplicationInfo
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'f';
    v[0x1] = 'g';
    v[0x2] = 'w';
    v[0x3] = 'E';
    v[0x4] = 'u';
    v[0x5] = 'v';
    v[0x6] = 'k';
    v[0x7] = 'a';
    v[0x8] = 'j';
    v[0x9] = 'k';
    v[0xa] = '\x7f';
    v[0xb] = 'e';
    v[0xc] = 'b';
    v[0xd] = '`';
    v[0xe] = 'F';
    v[0xf] = '~';
    v[0x10] = 'f';
    v[0x11] = 'n';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

static inline void fill_getApplicationInfo_signature(char v[]) {
    // (Ljava/lang/String;II)Landroid/content/pm/ApplicationInfo;
    static unsigned int m = 0;

    if (m == 0) {
        m = 53;
    } else if (m == 59) {
        m = 61;
    }

    v[0x0] = '-';
    v[0x1] = 'J';
    v[0x2] = 'm';
    v[0x3] = 'i';
    v[0x4] = '\x7f';
    v[0x5] = 'k';
    v[0x6] = '$';
    v[0x7] = '`';
    v[0x8] = 'l';
    v[0x9] = '`';
    v[0xa] = 'h';
    v[0xb] = '?';
    v[0xc] = 'B';
    v[0xd] = 'f';
    v[0xe] = 'a';
    v[0xf] = '}';
    v[0x10] = '{';
    v[0x11] = 'q';
    v[0x12] = ',';
    v[0x13] = 'Q';
    v[0x14] = 'P';
    v[0x15] = '3';
    v[0x16] = 'W';
    v[0x17] = '}';
    v[0x18] = 's';
    v[0x19] = 'z';
    v[0x1a] = 'm';
    v[0x1b] = 'O';
    v[0x1c] = 'H';
    v[0x1d] = 'F';
    v[0x1e] = '\x0c';
    v[0x1f] = 'G';
    v[0x20] = 'J';
    v[0x21] = 'H';
    v[0x22] = 'S';
    v[0x23] = 'M';
    v[0x24] = 'G';
    v[0x25] = '^';
    v[0x26] = '\x04';
    v[0x27] = '\\';
    v[0x28] = '@';
    v[0x29] = '\x01';
    v[0x2a] = 'n';
    v[0x2b] = '@';
    v[0x2c] = 'A';
    v[0x2d] = '^';
    v[0x2e] = 'Z';
    v[0x2f] = 'W';
    v[0x30] = 'a';
    v[0x31] = 'u';
    v[0x32] = 'k';
    v[0x33] = 'l';
    v[0x34] = 'j';
    v[0x35] = 'L';
    v[0x36] = 'h';
    v[0x37] = 'a';
    v[0x38] = 'g';
    v[0x39] = '2';
    for (unsigned int i = 0; i < 0x3a; ++i) {
        v[i] ^= ((i + 0x3a) % m);
    }
    v[0x3a] = '\0';
}

static inline void fill_sourceDir(char v[]) {
    // sourceDir
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = 'q';
    v[0x1] = 'l';
    v[0x2] = 'q';
    v[0x3] = 'w';
    v[0x4] = 'e';
    v[0x5] = 'e';
    v[0x6] = 'E';
    v[0x7] = 'k';
    v[0x8] = 'q';
    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    v[0x9] = '\0';
}

static inline void fill_sourceDir_signature(char v[]) {
    // Ljava/lang/String;
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'M';
    v[0x1] = 'h';
    v[0x2] = 'b';
    v[0x3] = 'r';
    v[0x4] = 'd';
    v[0x5] = ')';
    v[0x6] = 'k';
    v[0x7] = 'i';
    v[0x8] = 'g';
    v[0x9] = 'm';
    v[0xa] = '$';
    v[0xb] = '_';
    v[0xc] = 'y';
    v[0xd] = '|';
    v[0xe] = 'f';
    v[0xf] = '~';
    v[0x10] = 'g';
    v[0x11] = ':';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
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

static inline void fill_invalid_package_manager_path_s(char v[]) {
    // invalid package manager, path: %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 31;
    } else if (m == 37) {
        m = 41;
    }

    v[0x0] = 'k';
    v[0x1] = 'm';
    v[0x2] = 'r';
    v[0x3] = 'd';
    v[0x4] = 'j';
    v[0x5] = 'n';
    v[0x6] = 'l';
    v[0x7] = ')';
    v[0x8] = 'z';
    v[0x9] = 'j';
    v[0xa] = 'o';
    v[0xb] = 'f';
    v[0xc] = 'o';
    v[0xd] = 'h';
    v[0xe] = 'u';
    v[0xf] = '1';
    v[0x10] = '\x7f';
    v[0x11] = 'r';
    v[0x12] = 'z';
    v[0x13] = 't';
    v[0x14] = 'q';
    v[0x15] = 'r';
    v[0x16] = 'j';
    v[0x17] = '5';
    v[0x18] = ':';
    v[0x19] = 'k';
    v[0x1a] = '}';
    v[0x1b] = 'i';
    v[0x1c] = 'v';
    v[0x1d] = ':';
    v[0x1e] = '!';
    v[0x1f] = '\'';
    v[0x20] = 'p';
    for (unsigned int i = 0; i < 0x21; ++i) {
        v[i] ^= ((i + 0x21) % m);
    }
    v[0x21] = '\0';
}

char *getPath(JNIEnv *env, int uid, const char *packageName) {
    char *path = NULL;
    char v1[0x80], v2[0x80];

    if (packageName == NULL) {
        return NULL;
    }
    fill_android_os_ServiceManager(v2); // 0x19 + 1
    jclass classServiceManager = (*env)->FindClass(env, v2);
    if (classServiceManager == NULL) {
#ifdef DEBUG
        LOGW("cannot find ServiceManager");
#endif
        (*env)->ExceptionClear(env);
        return NULL;
    }
    debug(env, "ServiceManager: %s", classServiceManager);

    fill_getService(v1); // 0xa + 1
    fill_getService_signature(v2); // 0x28 + 1
    jmethodID method = (*env)->GetStaticMethodID(env, classServiceManager, v1, v2);
#ifdef DEBUG
    LOGI("ServiceManager.getService: %p", method);
#endif
    if (method == NULL) {
        (*env)->ExceptionClear(env);
        goto cleanClassServiceManager;
    }

    fill_package(v2); // 0x8 + 1
    jstring stringPackage = (*env)->NewStringUTF(env, v2);
    jobject service = (*env)->CallStaticObjectMethod(env, classServiceManager, method,
                                                     stringPackage);
    if (service == NULL) {
#ifdef DEBUG
        LOGW("cannot find package service");
#endif
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        goto cleanStringPackage;
    }
#ifdef DEBUG
    debug(env, "package service: %s", service);
#endif

    fill_android_content_pm_IPackageManager$Stub(v2); // 0x27 + 1
    jclass classIPackageManager$Stub = (*env)->FindClass(env, v2);
    debug(env, "IPackageManager$Stub: %s", classIPackageManager$Stub);
    if (classIPackageManager$Stub == NULL) {
#ifdef DEBUG
        LOGW("cannot find IPackageManager$Stub");
#endif
        (*env)->ExceptionClear(env);
        goto cleanService;
    }

    fill_asInterface(v1);
    fill_asInterface_signature(v2);
    method = (*env)->GetStaticMethodID(env, classIPackageManager$Stub, v1, v2);
#ifdef DEBUG
    LOGI("asInterface: %p", method);
#endif
    if (method == NULL) {
        (*env)->ExceptionClear(env);
        goto cleanClassIPackageManager$Stub;
    }

    jobject IPackageManager = (*env)->CallStaticObjectMethod(env, classIPackageManager$Stub, method,
                                                             service);
    if (IPackageManager == NULL) {
#ifdef DEBUG
        LOGW("cannot call asInterface");
#endif
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        goto cleanClassIPackageManager$Stub;
    }
    debug(env, "IPackageManager: %s", IPackageManager);

    // Landroid/content/pm/IPackageManager;->getApplicationInfo(Ljava/lang/String;II)Landroid/content/pm/ApplicationInfo;
    jclass classIPackageManager = (*env)->GetObjectClass(env, IPackageManager);
    debug(env, "classIPackageManager: %s", classIPackageManager);
    fill_getApplicationInfo(v1);
    fill_getApplicationInfo_signature(v2);
    method = (*env)->GetMethodID(env, classIPackageManager, v1, v2);
#ifdef DEBUG
    LOGI("getApplicationInfo: %p", method);
#endif
    if (method == NULL) {
        (*env)->ExceptionClear(env);
        goto cleanClassIPackageManager;
    }

    jstring stringPackageName = (*env)->NewStringUTF(env, packageName);
    int userId = uid / 100000;
    jobject applicationInfo = (*env)->CallObjectMethod(env, IPackageManager, method,
                                                       stringPackageName, 0, userId);
    debug(env, "applicationInfo: %s", applicationInfo);
    if (applicationInfo == NULL) {
        if ((*env)->ExceptionCheck(env)) {
#ifdef DEBUG
            (*env)->ExceptionDescribe(env);
#endif
            (*env)->ExceptionClear(env);
        }
        goto cleanStringPackageName;
    }

    jclass classApplicationInfo = (*env)->GetObjectClass(env, applicationInfo);
    debug(env, "class applicationInfo: %s", classApplicationInfo);

    fill_sourceDir(v1);
    fill_sourceDir_signature(v2);
    jfieldID field = (*env)->GetFieldID(env, classApplicationInfo, v1, v2);
#ifdef DEBUG
    LOGI("sourceDir: %p", field);
#endif
    if (field == NULL) {
        (*env)->ExceptionClear(env);
        goto cleanClassApplicationInfo;
    }
    jstring sourceDir = (*env)->GetObjectField(env, applicationInfo, field);
    debug(env, "sourceDir: %s", sourceDir);
    const char *chars = (*env)->GetStringUTFChars(env, sourceDir, NULL);

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

    jclass serviceClass = (*env)->GetObjectClass(env, service);
    debug(env, "service class: %s", serviceClass);
    if (classBinderProxy != NULL && !(*env)->IsSameObject(env, serviceClass, classBinderProxy)) {
        fill_invalid_package_manager_path_s(v2);
        LOGW(v2, chars);
    }
    path = strdup(chars);

#ifdef DEBUG
    LOGI("path: %s", chars);
#endif

    (*env)->DeleteLocalRef(env, serviceClass);
    if (classBinderProxy != NULL) {
        (*env)->DeleteLocalRef(env, classBinderProxy);
    }
    (*env)->ReleaseStringUTFChars(env, sourceDir, chars);
    (*env)->DeleteLocalRef(env, sourceDir);
cleanClassApplicationInfo:
    (*env)->DeleteLocalRef(env, classApplicationInfo);
    (*env)->DeleteLocalRef(env, applicationInfo);
cleanStringPackageName:
    (*env)->DeleteLocalRef(env, stringPackageName);
cleanClassIPackageManager:
    (*env)->DeleteLocalRef(env, classIPackageManager);
    (*env)->DeleteLocalRef(env, IPackageManager);
cleanClassIPackageManager$Stub:
    (*env)->DeleteLocalRef(env, classIPackageManager$Stub);
cleanService:
    (*env)->DeleteLocalRef(env, service);
cleanStringPackage:
    (*env)->DeleteLocalRef(env, stringPackage);
cleanClassServiceManager:
    (*env)->DeleteLocalRef(env, classServiceManager);

    return path;
}

