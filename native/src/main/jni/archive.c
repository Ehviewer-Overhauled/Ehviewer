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

#define LOG_TAG "libarchive_wrapper"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG ,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG ,__VA_ARGS__)

#define BLOCK_SIZE 4096 * 16

static JNIEnv *env;
static jbyteArray jbr;

static int cur_index = 0; // Current entry we haven't read any data from it yet
static struct archive* arc;

static void* archiveAddr = 0;
static jlong archiveSize = 0;

static void JNI_prepare_environment(JNIEnv* jniEnv)
{
    env = jniEnv;
    jbr = (*env)->NewGlobalRef(env, (*env)->NewByteArray(env, BLOCK_SIZE));
}

static int filename_is_playable_file(const char *name) {
    const char *dotptr = strrchr(name, '.');
    if (dotptr++) {
        switch (*dotptr) {
            case 'j':
                return (strcmp(dotptr, "jpg") == 0) || (strcmp(dotptr, "jpeg") == 0);
            case 'p':
                return strcmp(dotptr, "png") == 0;
            case 'g':
                return strcmp(dotptr, "gif") == 0;
            default:
                return 0;
        }
    }
    return 0;
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

static void archive_create() {
    arc = archive_read_new();
    archive_read_support_format_all(arc);
    archive_read_support_filter_all(arc);
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_openArchive(JNIEnv *_, jobject thiz, jlong addr, jlong size) {
    archiveAddr = (void *) addr;
    archiveSize = size;
    madvise(archiveAddr, archiveSize, MADV_SEQUENTIAL | MADV_WILLNEED);
    JNI_prepare_environment(_);
    archive_create();
    long r = archive_read_open_memory(arc, (const void *) archiveAddr, archiveSize);
    if (r) {
        r = 0;
        LOGE("%s%s", "Archive open failed:", archive_error_string(arc));
    } else {
        r = archive_list_all_entries();
        LOGI("%s%ld%s", "Found ", r, " image entries in archive");
    }
    archive_read_close(arc);
    archive_read_free(arc);
    arc = NULL;
    return r;
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_extracttoOutputStream(JNIEnv *_, jobject thiz, jint index, jint fd) {
    struct archive_entry *entry;
    size_t size;
    int ret;
    la_int64_t offset;
    const void* buff;
    if (!arc || index < cur_index) {
        if (arc)
            archive_read_close(arc);
        archive_read_free(arc);
        archive_create();
        archive_read_open_memory(arc, archiveAddr, archiveSize);
        cur_index = 0;
    }
    while (archive_read_next_header(arc, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (cur_index++ == index) {
            for (;;) {
                ret = archive_read_data_block(arc, &buff, &size, &offset);
                if (ret == ARCHIVE_EOF)
                    break;
                if (ret != ARCHIVE_OK) {
                    LOGE("%s%s", "Archive read failed:", archive_error_string(arc));
                    break;
                }
                write(fd, buff, size);
            }
            break;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_closeArchive(JNIEnv *jniEnv, jobject thiz) {
    if (arc) {
        archive_read_close(arc);
        archive_read_free(arc);
    }
}