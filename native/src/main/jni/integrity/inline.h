//
// Created by Thom on 2019/3/18.
//

#ifndef BREVENT_INLINE_H
#define BREVENT_INLINE_H

#include <stdbool.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifdef DEBUG_HOOK_IO
bool setRead(void *symbol);
#endif

bool isInlineHooked(void *symbol);

#ifdef DEBUG_HOOK_SELF
#if defined(__arm__) || defined(__aarch64__)

#include "hookzz/hookzz.h"

void check_inline_hook_hookzz();

void check_inline_hook_hookzz_b();

#endif

void check_inline_hook_whale();

#if defined(__arm__)

void check_inline_hook_substrate();

#endif
#endif

#ifdef __cplusplus
}
#endif

#endif //BREVENT_INLINE_H
