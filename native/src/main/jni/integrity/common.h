//
// Created by Thom on 2019/3/20.
//

#ifndef BREVENT_COMMON_H
#define BREVENT_COMMON_H

#include <jni.h>
#include <stdbool.h>
#include <android/log.h>

#if __has_include("genuine.h")
#include "genuine.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

enum {
    CHECK_TRUE,
    CHECK_FALSE,
    CHECK_FAKE,
    CHECK_OVERLAY,
    CHECK_ODEX,
    CHECK_DEX,
    CHECK_PROXY,
    CHECK_ERROR,
    CHECK_FATAL,
    CHECK_NOAPK,
};

#ifndef TAG
#define TAG "Genuine"
#endif

#ifndef LOGI
#define LOGI(...) (genuine_log_print(ANDROID_LOG_INFO, __VA_ARGS__))
#endif

#ifndef LOGW
#define LOGW(...) (genuine_log_print(ANDROID_LOG_WARN, __VA_ARGS__))
#endif

#ifndef LOGE
#define LOGE(...) (genuine_log_print(ANDROID_LOG_ERROR, __VA_ARGS__))
#endif

void genuine_log_print(int prio, const char *fmt, ...);

char *getGenuinePackageName();

bool setGenuine(JNIEnv *env, int genuine);

int getGenuine();

int getSdk();

bool has_native_libs();

#ifdef DEBUG

void debug(JNIEnv *env, const char *format, jobject object);

#else
#define debug(...) do {} while(0);
#endif

#ifdef __cplusplus
}
#endif

#endif //BREVENT_COMMON_H
