//
// Created by Thom on 2019/3/30.
//

#ifndef BREVENT_OPENAT_H
#define BREVENT_OPENAT_H

#include <stdint.h>
#include <fcntl.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __ANDROID__

intptr_t openAt(intptr_t fd, const char *path, intptr_t flag);

#else
#define openAt openat
#endif

#ifdef __cplusplus
}
#endif

#endif //BREVENT_OPENAT_H
