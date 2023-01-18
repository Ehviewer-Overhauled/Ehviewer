#include <jni.h>
#include <string.h>
#include <stdbool.h>
#include <android/log.h>

#define LOG_TAG "gifUtils"

#include "ehviewer.h"

#define GIF_HEADER_87A "GIF87a"
#define GIF_HEADER_89A "GIF89a"

static int FRAME_DELAY_START_MARKER = 0x0021F904;

typedef signed char byte;

#define FRAME_DELAY_START_MARKER ((byte*)(&FRAME_DELAY_START_MARKER))
#define MINIMUM_FRAME_DELAY 2
#define DEFAULT_FRAME_DELAY 10

static inline bool isGif(void *addr) {
    return !memcmp(addr, GIF_HEADER_87A, 6) || !memcmp(addr, GIF_HEADER_89A, 6);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_rewriteGifSource(JNIEnv *env, jclass clazz, jobject buffer) {
    byte *addr = (*env)->GetDirectBufferAddress(env, buffer);
    size_t size = (*env)->GetDirectBufferCapacity(env, buffer);
    if (size < 7 || !isGif(addr)) return;
    for (size_t i = 0; i < size - 8; i++) {
        if (addr[i] == FRAME_DELAY_START_MARKER[3] && addr[i + 1] == FRAME_DELAY_START_MARKER[2] &&
            addr[i + 2] == FRAME_DELAY_START_MARKER[1] &&
            addr[i + 3] == FRAME_DELAY_START_MARKER[0]) {
            byte *end = addr + i + 4;
            if (end[4] != 0) continue;
            int frameDelay = end[2] << 8 | end[1];
            if (frameDelay > MINIMUM_FRAME_DELAY)
                break; // Quit if the first block looks normal, for performance
            end[1] = DEFAULT_FRAME_DELAY;
            end[2] = 0;
        }
    }
}
