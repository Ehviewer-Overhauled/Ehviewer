//
// Created by Thom on 2019/3/18.
//

#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include "inline.h"
#include "common.h"

#ifdef DEBUG_HOOK_SELF

#include <syscall.h>
#include <unistd.h>
#include <sys/stat.h>
#include "whale/whale.h"

static long faccessat_replace(int dirfd, const char *pathname, int mode, int flags) {
    return syscall(__NR_faccessat, dirfd, pathname, mode, flags);
}

static long fchownat_replace(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags) {
    return syscall(__NR_fchownat, dirfd, pathname, owner, group, flags);
}

static int futimens_replace(int fd, const struct timespec times[2]) {
    return utimensat(fd, NULL, times, 0);
}

#if defined(__arm__) || defined(__aarch64__)

void check_inline_hook_hookzz() {
    void *backup;
    zz_disable_arm_arm64_b_branch();

    ZzReplace(&fchownat, &fchownat_replace, &backup);
    LOGI("hookzz fchownat: %p, replace: %p, hooked: %d", &fchownat, &fchownat_replace,
         isInlineHooked(&fchownat));

    ZzReplace(&faccessat, &faccessat_replace, &backup);
    LOGI("hookzz faccessat: %p, repalce: %p, hooked: %d", &faccessat, &faccessat_replace,
         isInlineHooked(&faccessat));

    ZzReplace(&futimens, &futimens_replace, &backup);
    LOGI("hookzz futimens: %p, replace: %p, hooked: %d", &futimens, &futimens_replace,
         isInlineHooked(&futimens));
}

void check_inline_hook_hookzz_b() {
    void *backup;
    zz_enable_arm_arm64_b_branch();

    ZzReplace(&fchownat, &fchownat_replace, &backup);
    LOGI("hookzz-b fchownat: %p, replace: %p, hooked: %d", &fchownat, &fchownat_replace,
         isInlineHooked(&fchownat));

    ZzReplace(&faccessat, &faccessat_replace, &backup);
    LOGI("hookzz-b faccessat: %p, repalce: %p, hooked: %d", &faccessat, &faccessat_replace,
         isInlineHooked(&faccessat));

    ZzReplace(&futimens, &futimens_replace, &backup);
    LOGI("hookzz-b futimens: %p, replace: %p, hooked: %d", &futimens, &futimens_replace,
         isInlineHooked(&futimens));
}

#endif

void check_inline_hook_whale() {
    WInlineHookFunction(&fchownat, &fchownat_replace, NULL);
    LOGI("whale fchownat: %p, replace: %p, hooked: %d", &fchownat, &fchownat_replace,
         isInlineHooked(&fchownat));

    WInlineHookFunction(&faccessat, &faccessat_replace, NULL);
    LOGI("whale faccessat: %p, repalce: %p, hooked: %d", &faccessat, &faccessat_replace,
         isInlineHooked(&faccessat));

    WInlineHookFunction(&futimens, &futimens_replace, NULL);
    LOGI("whale futimens: %p, replace: %p, hooked: %d", &futimens, &futimens_replace,
         isInlineHooked(&futimens));
}

#if defined(__arm__) || defined(DEBUG_HOOK_IDE)

#include "substrate/CydiaSubstrate.h"

void check_inline_hook_substrate() {
    MSHookFunction(&fchownat, &fchownat_replace, NULL);
    LOGI("substrate fchownat: %p, replace: %p, hooked: %d", &fchownat, &fchownat_replace,
         isInlineHooked(&fchownat));

    MSHookFunction(&faccessat, &faccessat_replace, NULL);
    LOGI("substrate faccessat: %p, repalce: %p, hooked: %d", &faccessat, &faccessat_replace,
         isInlineHooked(&faccessat));

    MSHookFunction(&futimens, &futimens_replace, NULL);
    LOGI("substrate futimens: %p, replace: %p, hooked: %d", &futimens, &futimens_replace,
         isInlineHooked(&futimens));
}

#endif

#endif

bool isInlineHooked(void *symbol) {
    if (symbol == NULL) {
        return false;
    }
#if defined(__arm__) || defined(DEBUG_HOOK_IDE)

// https://developer.arm.com/docs/ddi0597/b/base-instructions-alphabetic-order/ldr-literal-load-register-literal
// A1, !(P == 0 && W == 1), we don't check P and W
// cond 010P U0W1 1111 _Rt_ xxxx xxxx xxxx
#define IS_LDR_PC_A1(x) (((x) & 0xfe5ff000u) == 0xe41ff000u)
// T2
// 1111 1000 U101 1111 | _Rt_ xxxx xxxx xxxx
#define IS_LDR_PC_T2(x) (((x) & 0xf000ff7fu) == 0xf000f85fu)

// https://developer.arm.com/docs/ddi0597/b/base-instructions-alphabetic-order/b-branch
// A1
// cond 100 xxxx xxxx xxxx xxxx xxxx xxxx
#define IS_B_A1(x) (((x) & 0xff000000u) == 0xea000000u)
// T2
// 1110 0xxx xxxx xxxx
#define IS_B_T2(x) (((x) & 0xf800u) == 0xe000u)
// T4
// 1111 0Sxx xxxx xxxx | 10J1 Jxxx xxxx xxxx
//        -- imm10 --- |       --- imm11 ---
#define IS_B_T4(x) (((x) & 0xd000f800u) == 0x9000f000u)

// https://developer.arm.com/docs/ddi0597/b/base-instructions-alphabetic-order/nop-no-operation
// T1, hint should be 0000, we don't check
// 1011 1111 hint 0000
#define IS_NOP_T1(x) (((x) & 0xff0fu) == 0xbf00u)

// https://developer.arm.com/docs/ddi0597/b/base-instructions-alphabetic-order/mov-movs-register-move-register
// cydia use `mov r8, r8` for Nop
// T1, Mmmm is Rm, Dddd is Rd
// 0100 0110 DMmm mddd
#define _IS_MOV_T1(x) (((x) & 0xff00u) == 0x4600u)
#define _RM_MOV_T1(x) ((((x) & 0x78u) >> 3u))
#define _RD_MOV_T1(x) ((((x) & 0x80u) >> 4u) | ((x) & 7u))
#define IS_MOV_T1_RR(x) (_IS_MOV_T1(x) && _RM_MOV_T1(x) == _RD_MOV_T1(x))

// https://developer.arm.com/docs/ddi0597/b/base-instructions-alphabetic-order/bx-branch-and-exchange
// cydia use `bx`
// T1
// 0100	0111 0Rmm m000
#define IS_BX_T1(x) (((x) & 0xff87u) == 0x4700u)
#define RM_BX_T1(x) (((x) & 0x0078u) >> 3u)
#define IS_BX_PC_T1(x) ((x) == 0x4778u)

    uintptr_t address = (uintptr_t) symbol;
    if ((address & 1U) == 0) {
        uint32_t *value32 = (uint32_t *) address;
        if (IS_LDR_PC_A1(*value32)) {
#ifdef DEBUG_HOOK
            LOGW("(arm ldr pc) symbol: %p, value: %08x", symbol, *value32);
#endif
            return true;
        }
        if (IS_B_A1(*value32)) {
#ifdef DEBUG_HOOK
            LOGW("(arm b) symbol: %p, value: %08x", symbol, *value32);
#endif
            return true;
        }
#ifdef DEBUG
        LOGI("(arm) symbol: %p, value: %08x", symbol, *value32);
#endif
    } else {
        address = address & ~1U;
        uint16_t *value16 = (uint16_t *) address;
        uint32_t *value32 = (uint32_t *) address;
        if (IS_LDR_PC_T2(*value32)) {
#ifdef DEBUG_HOOK
            LOGW("(thumb ldr pc) symbol: %p, address: %p, value: %08x",
                 symbol, address, *value32);
#endif
            return true;
        }
        if (IS_B_T4(*value32)) {
#ifdef DEBUG_HOOK
            LOGW("(thumb b) symbol: %p, address: %p, value: %08x",
                 symbol, address, *value32);
#endif
            return true;
        }
        if (IS_B_T2(*value16)) {
#ifdef DEBUG_HOOK
            LOGW("(thumb b) symbol: %p, address: %p, value: %04x",
                 symbol, address, *value16);
#endif
            return true;
        }
        if (IS_NOP_T1(*value16) || IS_MOV_T1_RR(*value16)) {
#ifdef DEBUG_HOOK
            LOGW("(thumb nop) symbol: %p, address: %p, value: %04x",
                 symbol, address, *value16);
#endif
            address += 2;
            value16 = (uint16_t *) address;
            value32 = (uint32_t *) address;
        }
        if (IS_LDR_PC_T2(*value32)) {
#ifdef DEBUG_HOOK
            LOGW("(thumb ldr pc) symbol: %p, address: %p, value: %08x",
                 symbol, address, *value32);
#endif
            return true;
        }
        if (IS_BX_PC_T1(*value16) && IS_LDR_PC_A1(*(value32 + 1))) {
#ifdef DEBUG_HOOK
            LOGW("(thumb bx + arm ldr pc) symbol: %p, address: %p, value: %08x %08x",
                 symbol, address, *value32, *(value32 + 1));
#endif
            return true;
        }
#ifdef DEBUG
        LOGI("(thumb) symbol: %p, address: %p, value: %08x %08x",
             symbol, address, *value32, *(value32 + 1));
#endif
    }
#endif
#if defined(__aarch64__) || defined(DEBUG_HOOK_IDE)

// https://developer.arm.com/docs/ddi0596/latest/base-instructions-alphabetic-order/b-branch
// 0001 01xx xxxx xxxx xxxx xxxx xxxx xxxx
//        ------------ imm26 -------------
#define IS_B(x) (((x) & 0xfc000000u) == 0x14000000u)

// https://developer.arm.com/docs/ddi0596/latest/base-instructions-alphabetic-order/ldr-literal-load-register-literal
// 0101 1000 xxxx xxxx xxxx xxxx xxxR tttt
//           -------- imm19 --------
#define IS_LDR_X(x) (((x) & 0xff000000u) == 0x58000000u)
#define X_LDR(x) ((x) & 0x1fu)

// https://developer.arm.com/docs/ddi0596/latest/base-instructions-alphabetic-order/adrp-form-pc-relative-address-to-4kb-page
// 1xx1 0000 xxxx xxxx xxxx xxxx xxxR dddd
//  lo       -------- immhi --------
#define IS_ADRP_X(x) (((x) & 0x9f000000u) == 0x90000000u)
#define X_ADRP(x) ((x) & 0x1fu)

// https://developer.arm.com/docs/ddi0596/latest/base-instructions-alphabetic-order/br-branch-to-register
// 1101 0110 0001 1111 0000 00Rn nnn0 0000
#define IS_BR_X(x) (((x) & 0xfffffc0f) == 0xd61f0000u)
#define X_BR(x) (((x) & 0x3e0u) >> 0x5u)

// https://developer.arm.com/docs/ddi0596/latest/base-instructions-alphabetic-order/movz-move-wide-with-zero
// 1op1 0010 1hwx xxxx xxxx xxxx xxxR dddd
//              ------ imm16 -------
// for op, 00 -> MOVN, 10 -> MOVZ, 11 -> MOVK
#define IS_MOV_X(x) (((x) & 0x9f800000u) == 0x92800000u)
#define X_MOV(x) ((x) & 0x1fu)

    uint32_t *value32 = symbol;
    if (IS_B(*value32)) {
#ifdef DEBUG_HOOK
        LOGW("(arm64 b) symbol: %p, value: %08x", symbol, *value32);
#endif
        return true;
    }
    if (IS_LDR_X(*value32) && IS_BR_X(*(value32 + 1))) {
        uint32_t x = X_LDR(*value32);
        if (x == X_BR(*(value32 + 1))) {
#ifdef DEBUG_HOOK
            LOGW("(arm64 ldr+br x%d) symbol: %p, value: %08x %08x",
                 x, symbol, *value32, *(value32 + 1));
#endif
            return true;
        }
    }
    if (IS_ADRP_X(*value32) && IS_BR_X(*(value32 + 1))) {
        uint32_t x = X_ADRP(*value32);
        if (x == X_BR(*(value32 + 1))) {
#ifdef DEBUG_HOOK
            LOGW("(arm64 adrp+br x%d) symbol: %p, value: %08x %08x",
                 x, symbol, *value32, *(value32 + 1));
#endif
            return true;
        }
    }
    if (IS_MOV_X(*value32)) {
        uint32_t x = X_MOV(*value32);
        for (int i = 1; i <= 4; ++i) {
            if (IS_BR_X(*(value32 + i))) {
                if (x != X_BR(*(value32 + i))) {
                    break;
                }
#ifdef DEBUG_HOOK
                for (int k = 0; k < i; ++k) {
                    LOGW("(arm64 mov x%d) symbol: %p, value: %08x",
                         x, symbol + sizeof(uint32_t) * k, *(value32 + k));
                }
                LOGW("(arm64  br x%d) symbol: %p, value: %08x",
                     x, symbol + sizeof(uint32_t) * i, *(value32 + i));
#endif
                return true;
            } else if (IS_MOV_X(*(value32 + i))) {
                if (x != X_MOV(*(value32 + i))) {
                    break;
                }
            }
        }
    }
#ifdef DEBUG
    LOGI("(arm64) symbol: %p, value: %08x %08x", symbol, *value32, *(value32 + 1));
#endif
#endif
    return false;
}

#ifdef DEBUG_HOOK_IO
bool setRead(void *symbol) {
    uintptr_t address = (uintptr_t) symbol;
    uintptr_t page_size = (uintptr_t) getpagesize();
    uintptr_t base = address & ~(page_size - 1);
    // inline check read at most 20 bytes
    uintptr_t end = (address + 20 + page_size - 1) & -page_size;
#ifdef DEBUG
    LOGI("set r+x from %p to %p", base, end);
#endif
    if (mprotect((void *) base, end - base, PROT_READ | PROT_EXEC)) {
#ifdef DEBUG
        LOGW("cannot mprotect: %s", strerror(errno));
#endif
        return false;
    } else {
        return true;
    }
}
#endif
