//
// Created by Thom on 2019/3/15.
//

#ifndef BREVENT_AM_PROXY_H
#define BREVENT_AM_PROXY_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

bool isAmProxy(JNIEnv *env, int sdk);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_AM_PROXY_H
