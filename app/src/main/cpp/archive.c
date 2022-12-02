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

#include <assert.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <pthread.h>
#include <sys/mman.h>
#include <syscall.h>

#include <jni.h>
#include <android/log.h>

#include <archive.h>
#include <archive_entry.h>

#define LOG_TAG "libarchive_wrapper"

#include "ehviewer.h"

typedef struct {
    int using;
    int next_index;
    struct archive *arc;
    struct archive_entry *entry;
} archive_ctx;

pthread_mutex_t ctx_lock;
static archive_ctx **ctx_pool;
#define CTX_POOL_SIZE 20

static bool need_encrypt = false;
static char *passwd = NULL;
static void *archiveAddr = NULL;
static size_t archiveSize = 0;

#define POPULATE_THRESHOLD (512 * 1024 * 1024) // 512 MiB

const char supportExt[9][6] = {
        "jpeg",
        "jpg",
        "png",
        "gif",
        "webp",
        "bmp",
        "ico",
        "wbmp",
        "heif"
};

static inline int filename_is_playable_file(const char *name) {
    const char *dotptr = strrchr(name, '.');
    if (!dotptr++)
        return false;
    int i;
    for (i = 0; i < 9; i++)
        if (strcmp(dotptr, supportExt[i]) == 0)
            return true;
    return false;
}

static long archive_list_all_entries(archive_ctx *ctx) {
    long count = 0;
    while (archive_read_next_header(ctx->arc, &ctx->entry) == ARCHIVE_OK)
        if (filename_is_playable_file(archive_entry_pathname(ctx->entry)))
            count++;
    if (!count)
        LOGE("%s", archive_error_string(ctx->arc));
    return count;
}

static void archive_release_ctx(archive_ctx *ctx) {
    if (ctx) {
        archive_read_close(ctx->arc);
        archive_read_free(ctx->arc);
        free(ctx);
    }
}

static int archive_alloc_ctx(archive_ctx **ctxptr) {
    archive_ctx *ctx;
    int err;
    ctx = calloc(1, sizeof(archive_ctx));
    if (!ctx)
        return -ENOMEM;
    ctx->arc = archive_read_new();
    ctx->using = 1;
    if (!ctx->arc) {
        free(ctx);
        return -ENOMEM;
    }
    archive_read_support_format_tar(ctx->arc);
    archive_read_support_format_7zip(ctx->arc);
    archive_read_support_format_rar5(ctx->arc);
    archive_read_support_format_zip(ctx->arc);
    archive_read_support_filter_gzip(ctx->arc);
    archive_read_support_filter_xz(ctx->arc);
    archive_read_set_option(ctx->arc, "zip", "ignorecrc32", "1");
    if (passwd)
        archive_read_add_passphrase(ctx->arc, passwd);
    err = archive_read_open_memory(ctx->arc, archiveAddr, archiveSize);
    if (err) {
        archive_read_free(ctx->arc);
        free(ctx);
        return err;
    }
    *ctxptr = ctx;
    return 0;
}

static int archive_skip_to_index(archive_ctx *ctx, int index) {
    assert(!(ctx->next_index > index));
    while (archive_read_next_header(ctx->arc, &ctx->entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(ctx->entry)))
            continue;
        if (ctx->next_index++ == index) {
            return ctx->next_index - 1;
        }
    }
    return ARCHIVE_FATAL;
}

static int archive_get_ctx(archive_ctx **ctxptr, int idx) {
    int ret;
    archive_ctx *ctx = NULL;
    pthread_mutex_lock(&ctx_lock);
    for (int i = 0; i < CTX_POOL_SIZE; i++) {
        if (!ctx_pool[i])
            continue;
        if (ctx_pool[i]->using)
            continue;
        if (ctx_pool[i]->next_index > idx)
            continue;
        if (!ctx || ctx_pool[i]->next_index > ctx->next_index)
            ctx = ctx_pool[i];
        if (ctx->next_index == idx)
            break;
    }
    if (ctx)
        ctx->using = 1;
    pthread_mutex_unlock(&ctx_lock);

    if (!ctx) {
        archive_ctx *victimCtx = NULL;
        int victimIdx = 0;
        int replace = 1;
        ret = archive_alloc_ctx(&ctx);
        if (ret)
            return ret;
        pthread_mutex_lock(&ctx_lock);
        for (int i = 0; i < CTX_POOL_SIZE; i++) {
            if (!ctx_pool[i]) {
                ctx_pool[i] = ctx;
                replace = 0;
                break;
            }
            if (ctx_pool[i]->using)
                continue;
            if (!victimCtx || ctx_pool[i]->next_index > victimCtx->next_index) {
                victimCtx = ctx_pool[i];
                victimIdx = i;
            }
        }
        if (replace) {
            archive_release_ctx(victimCtx);
            ctx_pool[victimIdx] = ctx;
        }
        pthread_mutex_unlock(&ctx_lock);
    }
    ret = archive_skip_to_index(ctx, idx);
    if (ret != idx) {
        ret = archive_errno(ctx->arc);
        LOGE("Skip to index failed:%s", archive_error_string(ctx->arc));
        archive_release_ctx(ctx);
        return ret;
    }
    *ctxptr = ctx;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_openArchive(JNIEnv *env, jobject thiz, jint fd, jlong size) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    archive_ctx *ctx = NULL;
    int mmap_flags = MAP_PRIVATE;
    if (size <= POPULATE_THRESHOLD)
        mmap_flags |= MAP_POPULATE;
    archiveAddr = mmap64(0, size, PROT_READ, mmap_flags, fd, 0);
    if (archiveAddr == MAP_FAILED) {
        LOGE("%s%s", "mmap64 failed with error ", strerror(errno));
        return 0;
    }
    archiveSize = size;
    madvise_log_if_error(archiveAddr, archiveSize, MADV_WILLNEED);
    pthread_mutex_init(&ctx_lock, 0);
    ctx_pool = calloc(CTX_POOL_SIZE, sizeof(archive_ctx **));
    if (!ctx_pool) {
        LOGE("Allocate archive ctx pool failed:ENOMEM");
        return 0;
    }
    long r = archive_alloc_ctx(&ctx);
    if (r) {
        if (r == -ENOMEM)
            LOGE("%s", "Mem alloc failed");
        r = 0;
        LOGE("%s%s", "Archive open failed:", archive_error_string(ctx->arc));
    } else {
        r = archive_list_all_entries(ctx);
        LOGI("%s%ld%s", "Found ", r, " image entries in archive");

        // We must read through the file|vm then we can know whether it is encrypted
        int encryptRet = archive_read_has_encrypted_entries(ctx->arc);
        switch (encryptRet) {
            case 1: // At lease 1 encrypted entry
                need_encrypt = true;
                break;
            case 0: // format supports but no encrypted entry found
            default:
                need_encrypt = false;
        }

        int format = archive_format(ctx->arc);
        switch (format) {
            case ARCHIVE_FORMAT_ZIP:
            case ARCHIVE_FORMAT_RAR_V5:
                madvise_log_if_error(archiveAddr, archiveSize, MADV_SEQUENTIAL);
                break;
            case ARCHIVE_FORMAT_7ZIP: // Seek is bad
                madvise_log_if_error(archiveAddr, archiveSize, MADV_RANDOM);
                break;
            default:;
        }
    }

    archive_release_ctx(ctx);
    return r;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_UriArchiveAccessor_extractToByteBuffer(JNIEnv *env, jobject thiz, jint index) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    archive_ctx *ctx = NULL;
    int ret;
    ret = archive_get_ctx(&ctx, index);
    if (ret)
        return 0;
    long long size = archive_entry_size(ctx->entry);
    void *buffer = malloc(size);
    if (!buffer) {
        ctx->using = 0;
        LOGE("Allocate buffer for decompression failed:ENOMEM");
        return 0;
    }
    ret = archive_read_data(ctx->arc, buffer, size);
    ctx->using = 0;
    if (ret == size)
        return (*env)->NewDirectByteBuffer(env, buffer, size);
    if (ret != size)
        LOGE("%s", "No enough data read, WTF?");
    if (ret < 0)
        LOGE("%s%s", "Archive read failed:", archive_error_string(ctx->arc));
    free(buffer);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_closeArchive(JNIEnv *env, jobject thiz) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    for (int i = 0; i < CTX_POOL_SIZE; i++)
        archive_release_ctx(ctx_pool[i]);
    free(passwd);
    free(ctx_pool);
    pthread_mutex_destroy(&ctx_lock);
    passwd = NULL;
    need_encrypt = false;
    munmap(archiveAddr, archiveSize);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_UriArchiveAccessor_needPassword(JNIEnv *env, jobject thiz) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    return need_encrypt;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_UriArchiveAccessor_providePassword(JNIEnv *env, jobject thiz, jstring str) {
    EH_UNUSED(thiz);
    struct archive_entry *entry;
    archive_ctx *ctx;
    jboolean ret = true;
    int len = (*env)->GetStringUTFLength(env, str);
    passwd = realloc(passwd, len * sizeof(char));
    if (!passwd) {
        LOGE("Allocate passwd buffer failed");
        return false;
    }
    strcpy(passwd, (*env)->GetStringUTFChars(env, str, NULL));
    archive_alloc_ctx(&ctx);
    void *tmpBuf = alloca(4096);
    while (archive_read_next_header(ctx->arc, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (!archive_entry_is_encrypted(entry))
            continue;
        if (archive_read_data(ctx->arc, tmpBuf, 4096) < ARCHIVE_OK) {
            LOGE("%s%s", "Archive read failed:", archive_error_string(ctx->arc));
            ret = false;
        }
        break;
    }
    archive_release_ctx(ctx);
    return ret;
}

JNIEXPORT jstring JNICALL
Java_com_hippo_UriArchiveAccessor_getFilename(JNIEnv *env, jobject thiz, jint index) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    archive_ctx *ctx = NULL;
    int ret;
    ret = archive_get_ctx(&ctx, index);
    if (ret)
        return NULL;
    jstring str = (*env)->NewStringUTF(env, archive_entry_pathname(ctx->entry));
    ctx->using = 0;
    return str;
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_extractToFd(JNIEnv *env, jobject thiz, jint index, jint fd) {
    EH_UNUSED(env);
    EH_UNUSED(thiz);
    archive_ctx *ctx = NULL;
    int ret;
    ret = archive_get_ctx(&ctx, index);
    if (!ret) {
        archive_read_data_into_fd(ctx->arc, fd);
        ctx->using = 0;
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_releaseByteBuffer(JNIEnv *env, jobject thiz, jobject buffer) {
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    free(addr);
}