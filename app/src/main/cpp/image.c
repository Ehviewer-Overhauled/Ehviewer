/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
 */

#include <stdlib.h>
#include <string.h>

#include <android/data_space.h>
#include <android/imagedecoder.h>
#include <android/log.h>

#include <GLES2/gl2.h>

#include "image.h"

#define TAG "ImageDecoder_wrapper"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG ,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)

#define IMAGE_TILE_MAX_SIZE (512 * 512)

static void *tile_buffer = NULL;

bool image_onLoad() {
    tile_buffer = malloc(IMAGE_TILE_MAX_SIZE * 4);
    return tile_buffer != NULL;
}

void image_onUnload() {
    free(tile_buffer);
    tile_buffer = NULL;
}

typedef struct {
    AImageDecoder *decoder;
    void *srcBuffer;
    size_t stride;
    int fd;
} SOURCE;

typedef struct {
    int32_t width;
    int32_t height;
    void *buffer;
    int bufferLen;
    bool isAnimated;
    bool alpha;
    int delay;
    SOURCE *src;
} IMAGE;

static void recycleSource(SOURCE *src) {
    if (!src)
        return;
    AImageDecoder_delete(src->decoder);
    free(src->srcBuffer);
    free(src);
}

static void recycle(IMAGE *image) {
    if (!image)
        return;
    recycleSource(image->src);
    free(image->buffer);
    free(image);
}

bool copy_pixels(const void *src, int src_w, int src_h, int src_x, int src_y,
                 void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
                 int width, int height) {
    int left;
    int line;
    size_t line_stride;
    int src_stride;
    int src_pos;
    int dst_pos;
    size_t dst_blank_length;

    // Sanitize
    if (src_x < 0) {
        width -= src_x;
        dst_x -= src_x;
        src_x = 0;
    }
    if (dst_x < 0) {
        width -= dst_x;
        src_x -= dst_x;
        dst_x = 0;
    }
    if (width <= 0) {
        return false;
    }
    if (src_y < 0) {
        height -= src_y;
        dst_y -= src_y;
        src_y = 0;
    }
    if (dst_y < 0) {
        height -= dst_y;
        src_y -= dst_y;
        dst_y = 0;
    }
    if (height <= 0) {
        return false;
    }
    left = src_x + width - src_w;
    if (left > 0) {
        width -= left;
    }
    left = dst_x + width - dst_w;
    if (left > 0) {
        width -= left;
    }
    if (width <= 0) {
        return false;
    }
    left = src_y + height - src_h;
    if (left > 0) {
        height -= left;
    }
    left = dst_y + height - dst_h;
    if (left > 0) {
        height -= left;
    }
    if (height <= 0) {
        return false;
    }

    // Init
    line_stride = (size_t) (width * 4);
    src_stride = src_w * 4;
    src_pos = src_y * src_stride + src_x * 4;
    dst_pos = 0;

    dst_blank_length = (size_t) (dst_y * dst_w + dst_x) * 4;

    // First line
    dst_pos += (int) dst_blank_length;
    memcpy(dst + dst_pos, src + src_pos, line_stride);
    dst_pos += (int) line_stride;
    src_pos += src_stride;

    // Other lines
    dst_blank_length = (size_t) ((dst_w - width) * 4);
    for (line = 1; line < height; line++) {
        dst_pos += (int) dst_blank_length;
        memcpy(dst + dst_pos, src + src_pos, line_stride);
        dst_pos += (int) line_stride;
        src_pos += src_stride;
    }

    return true;
}

void decodeCommon(IMAGE *image, SOURCE *src) {
    image->src = src;
    AImageDecoder *decoder = src->decoder;
    image->isAnimated = AImageDecoder_isAnimated(decoder);
    AImageDecoder_setDataSpace(decoder, ADATASPACE_SRGB);
    AImageDecoder_setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);
    const AImageDecoderHeaderInfo *headerInfo = AImageDecoder_getHeaderInfo(decoder);
    image->height = AImageDecoderHeaderInfo_getHeight(headerInfo);
    image->width = AImageDecoderHeaderInfo_getWidth(headerInfo);
    image->alpha =
            AImageDecoderHeaderInfo_getAlphaFlags(headerInfo) == ANDROID_BITMAP_FLAGS_ALPHA_PREMUL;
    src->stride = AImageDecoder_getMinimumStride(decoder);
    image->bufferLen = (int) (image->height * src->stride);
    image->buffer = malloc(image->bufferLen);
    if (image->isAnimated) {
        AImageDecoder_setInternallyHandleDisposePrevious(decoder, false);
        AImageDecoderFrameInfo *frameInfo = AImageDecoderFrameInfo_create();
        AImageDecoder_getFrameInfo(decoder, frameInfo);
        image->delay = (int) AImageDecoderFrameInfo_getDuration(frameInfo) / 1000000;
        AImageDecoderFrameInfo_delete(frameInfo);
    }
    AImageDecoder_decodeImage(decoder, image->buffer, src->stride, image->bufferLen);
    if (!image->isAnimated) {
        recycleSource(image->src);
        image->src = NULL;
    }
}

IMAGE *createFromFd(int fd) {
    AImageDecoder *decoder = NULL;
    if (AImageDecoder_createFromFd(fd, &decoder) != ANDROID_IMAGE_DECODER_SUCCESS)
        return NULL;
    IMAGE *image = calloc(1, sizeof(IMAGE));
    SOURCE *src = calloc(1, sizeof(SOURCE));
    src->decoder = decoder;
    src->fd = fd;
    decodeCommon(image, src);
    return image;
}

IMAGE *createFromAddr(void *addr, long long size) {
    AImageDecoder *decoder;
    AImageDecoder_createFromBuffer((const void *) addr + sizeof(long long), size, &decoder);
    IMAGE *image = calloc(1, sizeof(IMAGE));
    SOURCE *src = calloc(1, sizeof(SOURCE));
    src->decoder = decoder;
    src->srcBuffer = addr;
    decodeCommon(image, src);
    return image;
}

IMAGE *create(int32_t width, int32_t height, const void *data) {
    IMAGE *plain = calloc(1, sizeof(IMAGE));
    void *buffer = NULL;
    size_t length = width * height * 4;

    buffer = malloc(length);
    memcpy(buffer, data, length);

    // plain->isAnimated = false; this is unnecessary since we use calloc
    plain->width = width;
    plain->height = height;
    plain->buffer = buffer;
    plain->bufferLen = (int) length;

    return plain;
}

void render(IMAGE *image, int src_x, int src_y,
            void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
            int width, int height) {
    copy_pixels(image->buffer, image->width, image->height, src_x, src_y, dst, dst_w, dst_h, dst_x,
                dst_y, width, height);
}

void advance(IMAGE *image) {
    if (image->isAnimated) {
        AImageDecoder_advanceFrame(image->src->decoder);
        if (AImageDecoder_decodeImage(image->src->decoder, image->buffer, image->src->stride,
                                      image->bufferLen) == ANDROID_IMAGE_DECODER_FINISHED) {
            AImageDecoder_rewind(image->src->decoder);
            AImageDecoder_decodeImage(image->src->decoder, image->buffer, image->src->stride,
                                      image->bufferLen);
        }
    }
}

JNIEXPORT jlong JNICALL
Java_com_hippo_image_Image_nativeDecode(JNIEnv *env, jclass clazz, jint fd) {
    IMAGE *image = createFromFd(fd);
    return (jlong) image;
}

JNIEXPORT jlong JNICALL
Java_com_hippo_image_Image_nativeCreate(JNIEnv *env, jclass clazz, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels = NULL;
    void *image = NULL;

    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    image = create((int) info.width, (int) info.height, pixels);
    AndroidBitmap_unlockPixels(env, bitmap);
    return (jlong) image;
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRender(JNIEnv *env, jclass clazz, jlong ptr, jint src_x,
                                        jint src_y, jobject dst, jint dst_x, jint dst_y, jint width,
                                        jint height) {
    AndroidBitmapInfo info;
    void *pixels = NULL;

    AndroidBitmap_getInfo(env, dst, &info);
    AndroidBitmap_lockPixels(env, dst, &pixels);

    render((IMAGE *) ptr, src_x, src_y, pixels, (int) info.width, (int) info.height, dst_x, dst_y,
           width,
           height);

    AndroidBitmap_unlockPixels(env, dst);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeTexImage(JNIEnv *env, jclass clazz, jlong ptr, jboolean init,
                                          jint src_x, jint src_y, jint width, jint height) {
    if (width * height > IMAGE_TILE_MAX_SIZE)
        return;

    render((IMAGE *) ptr, src_x, src_y, tile_buffer, width, height, 0, 0, width, height);

    if (init) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                     tile_buffer);
    } else {
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE,
                        tile_buffer);
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeAdvance(JNIEnv *env, jclass clazz, jlong ptr) {
    advance((IMAGE *) ptr);
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetDelay(JNIEnv *env, jclass clazz, jlong ptr) {
    return ((IMAGE *) ptr)->delay;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_image_Image_nativeIsOpaque(JNIEnv *env, jclass clazz, jlong ptr) {
    return ((IMAGE *) ptr)->alpha;
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRecycle(JNIEnv *env, jclass clazz, jlong ptr) {
    recycle((IMAGE *) ptr);
}

JNIEXPORT jlong JNICALL
Java_com_hippo_image_Image_nativeDecodeAddr(JNIEnv *env, jclass clazz, jlong addr) {
    IMAGE *image = createFromAddr((void *) addr, *(long long *) addr);
    return (jlong) image;
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetFormat(JNIEnv *env, jclass clazz, jlong native_ptr) {
    return ((IMAGE *) native_ptr)->isAnimated;
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetWidth(JNIEnv *env, jclass clazz, jlong native_ptr) {
    return ((IMAGE *) native_ptr)->width;
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetHeight(JNIEnv *env, jclass clazz, jlong native_ptr) {
    return ((IMAGE *) native_ptr)->height;
}
