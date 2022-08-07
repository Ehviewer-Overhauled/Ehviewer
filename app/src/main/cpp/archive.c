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

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include <archive.h>
#include <archive_entry.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <stdbool.h>

#define LOG_TAG "libarchive_wrapper"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG ,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG ,__VA_ARGS__)

static JNIEnv *env;
static int cur_index = 0; // Current entry we haven't read any data from it yet
static struct archive *arc = NULL;
static bool need_encrypt = false;
static char *passwd = NULL;
static void *archiveAddr = NULL;
static jlong archiveSize = 0;

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

static int filename_is_playable_file(const char *name) {
    const char *dotptr = strrchr(name, '.');
    if (!dotptr++)
        return false;
    int i;
    for (i = 0; i < 9; i++)
        if (strcmp(dotptr, supportExt[i]) == 0)
            return true;
    return false;
}

static long archive_list_all_entries() {
    struct archive_entry *entry;
    long count = 0;
    while (archive_read_next_header(arc, &entry) == ARCHIVE_OK) {
        if (filename_is_playable_file(archive_entry_pathname(entry)))
            count++;
    }
    if (!count)
        LOGE("%s", archive_error_string(arc));
    return count;
}

static void archive_release() {
    if (arc) {
        archive_read_close(arc);
        archive_read_free(arc);
        arc = NULL;
    }
}

static int archive_alloc() {
    archive_release();
    arc = archive_read_new();
    archive_read_support_format_all(arc);
    archive_read_support_filter_all(arc);
    archive_read_set_option(arc, "zip", "ignorecrc32", "1");
    if (passwd)
        archive_read_add_passphrase(arc, passwd);
    return archive_read_open_memory(arc, archiveAddr, archiveSize);
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_openArchive(JNIEnv *_, jobject thiz, jlong addr, jlong size) {
    archiveAddr = (void *) addr;
    archiveSize = size;
    env = _;
    madvise(archiveAddr, archiveSize, MADV_SEQUENTIAL | MADV_WILLNEED);
    long r = archive_alloc();
    if (r) {
        r = 0;
        LOGE("%s%s", "Archive open failed:", archive_error_string(arc));
    } else {
        r = archive_list_all_entries();
        LOGI("%s%ld%s", "Found ", r, " image entries in archive");

        // We must read through the file|vm then we can know whether it is encrypted
        int encryptRet = archive_read_has_encrypted_entries(arc);
        switch (encryptRet) {
            case 1: // At lease 1 encrypted entry
                need_encrypt = true;
                break;
            case 0: // format supports but no encrypted entry found
            default:
                need_encrypt = false;
        }
        LOGI("%s%d", "archive_read_has_encrypted_entries returns ", encryptRet);
    }
    archive_release();
    return r;
}

typedef struct Memarea {
    void *buffer;
    long size;
} Memarea;

JNIEXPORT jlong JNICALL
Java_com_hippo_UriArchiveAccessor_extracttoOutputStream(JNIEnv *_, jobject thiz, jint index) {
    struct archive_entry *entry;
    int ret;
    if (!arc || index < cur_index) {
        archive_alloc();
        cur_index = 0;
    }
    Memarea *memarea = malloc(sizeof(Memarea));
    while (archive_read_next_header(arc, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (cur_index++ == index) {
            memarea->size = archive_entry_size(entry);
            memarea->buffer = malloc(memarea->size);
            ret = archive_read_data(arc, memarea->buffer, memarea->size);
            if (ret != memarea->size)
                LOGE("%s", "No enough data read, WTF?");
            if (ret < 0)
                LOGE("%s%s", "Archive read failed:", archive_error_string(arc));
            break;
        }
    }
    return (jlong) memarea;
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_closeArchive(JNIEnv *jniEnv, jobject thiz) {
    archive_release();
    free(passwd);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_UriArchiveAccessor_needPassword(JNIEnv *_, jobject thiz) {
    return need_encrypt;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_UriArchiveAccessor_providePassword(JNIEnv *_, jobject thiz, jstring str) {
    struct archive_entry *entry;
    jboolean ret = true;
    int len = (*env)->GetStringUTFLength(env, str);
    if (passwd)
        free(passwd);
    passwd = calloc(len, sizeof(char));
    strcpy(passwd, (*env)->GetStringUTFChars(env, str, NULL));
    archive_alloc();
    while (archive_read_next_header(arc, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (!archive_entry_is_encrypted(entry))
            continue;
        void *tmpBuf = malloc(4096);
        if (archive_read_data(arc, tmpBuf, 4096) < ARCHIVE_OK) {
            LOGE("%s%s", "Archive read failed:", archive_error_string(arc));
            ret = false;
        }
        free(tmpBuf);
        break;
    }
    archive_release();
    return ret;
}
