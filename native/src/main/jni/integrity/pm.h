//
// Created by Thom on 2019/3/15.
//

#ifndef BREVENT_PM_H
#define BREVENT_PM_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

char *getPath(JNIEnv *env, int uid, const char *name);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_PM_H
