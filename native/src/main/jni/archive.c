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

#define LOG_TAG "libarchive_wrapper"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG ,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG ,__VA_ARGS__)

#define BLOCK_SIZE 4096 * 16

static JNIEnv *env;
static jobject file;
static jmethodID readID;
static jmethodID seekID;
static jmethodID skipID;
static jmethodID rewindID;
static void* read_buffer;
jbyteArray jbr;

static void JNI_prepare_environment(JNIEnv* jniEnv, jobject OsReadablefile)
{
    env = jniEnv;
    file = (*env)->NewGlobalRef(env, OsReadablefile);
    readID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, OsReadablefile), "read", "([B)I");
    seekID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, OsReadablefile), "seek", "(JI)J");
    skipID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, OsReadablefile), "skip", "(J)J");
    rewindID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, OsReadablefile), "rewind", "()V");
    jbr = (*env)->NewGlobalRef(env, (*env)->NewByteArray(env, BLOCK_SIZE));
    read_buffer = malloc(BLOCK_SIZE);
}

static void JNI_destroy_environment()
{
    (*env)->DeleteGlobalRef(env, file);
    (*env)->DeleteGlobalRef(env, jbr);
    free(read_buffer);
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

static long archive_list_all_entries(struct archive *arc) {
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

ssize_t ins_read(struct archive *a, void *client_data_ptr, const void **buff) {
    int32_t r = (*env)->CallIntMethod(env, file, readID, jbr);
    (*env)->GetByteArrayRegion(env, jbr, 0, r, read_buffer);
    *buff = read_buffer;
    return r;
}

int ins_close(struct archive *a, void *client_data) {
    (*env)->CallVoidMethod(env, file, rewindID);
    return ARCHIVE_OK;
}

la_int64_t ins_seek(struct archive *a, void *client_data, la_int64_t offset, int whence) {
    jlong ret = (*env)->CallLongMethod(env, file, seekID, offset, whence);
    return ret;
}

la_int64_t ins_skip(struct archive *a, void *client_data, la_int64_t request) {
    jlong ret = (*env)->CallLongMethod(env, file, skipID, request);
    return ret;
}

static struct archive *archive_create() {
    struct archive* arc = archive_read_new();
    archive_read_set_seek_callback(arc, ins_seek);
    archive_read_set_skip_callback(arc, ins_skip);
    archive_read_support_format_all(arc);
    archive_read_support_filter_all(arc);
    return arc;
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_openArchive(JNIEnv *_, jobject thiz, jobject osf) {
    JNI_prepare_environment(_, osf);
    struct archive *arc = archive_create();
    long r = archive_read_open(arc, arc, NULL, ins_read, ins_close);
    if (r) {
        r = 0;
        LOGE("%s%s", "Archive open failed:", archive_error_string(arc));
    } else {
        r = archive_list_all_entries(arc);
        LOGI("%s%ld%s", "Found ", r, " image entries in archive");
    }
    archive_read_close(arc);
    archive_read_free(arc);
    return r;
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_extracttoOutputStream(JNIEnv *_, jobject thiz, jint index, jobject os) {
    int count = 0;
    struct archive_entry *entry;
    struct archive *arc = archive_create();
    size_t size;
    int ret;
    la_int64_t offset;
    const void* buff;
    jmethodID writeID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "write", "([B)V");
    jmethodID flushID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "flush", "()V");
    jmethodID closeID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "close", "()V");
    archive_read_open(arc, arc, NULL, ins_read, ins_close);
    while (archive_read_next_header(arc, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (count++ == index) {
            for (;;) {
                ret = archive_read_data_block(arc, &buff, &size, &offset);
                if (ret == ARCHIVE_EOF)
                    break;
                if (ret != ARCHIVE_OK) {
                    LOGE("%s%s", "Archive read failed:", archive_error_string(arc));
                    break;
                }
                (*env)->SetByteArrayRegion(env, jbr, 0, (int)size, buff);
                (*env)->CallVoidMethod(env, os, writeID, jbr);
            }
            break;
        }
    }
    (*env)->CallVoidMethod(env, os, flushID);
    (*env)->CallVoidMethod(env, os, closeID);
    archive_read_close(arc);
    archive_read_free(arc);
}

JNIEXPORT void JNICALL
Java_com_hippo_UriArchiveAccessor_closeArchive(JNIEnv *jniEnv, jobject thiz) {
    JNI_destroy_environment();
}