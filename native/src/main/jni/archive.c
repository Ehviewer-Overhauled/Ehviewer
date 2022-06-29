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

typedef struct archive_inst_sc {
    struct archive *archive;
    JNIEnv *env;
    jobject file;
    jmethodID readID;
    jmethodID seekID;
    jmethodID skipID;
    void *read_buffer;
    void *output_buffer;
    jbyteArray jbr;
} archive_inst;

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

static long archive_list_all_entries(struct archive_inst_sc *inst) {
    struct archive *a = inst->archive;
    struct archive_entry *entry;
    long count = 0;
    while (archive_read_next_header(a, &entry) == ARCHIVE_OK) {
        if (filename_is_playable_file(archive_entry_pathname(entry)))
            count++;
    }
    if (!count)
        LOGE("%s", archive_error_string(inst->archive));
    return count;
}

ssize_t ins_read(struct archive *a, void *client_data_ptr, const void **buff) {
    archive_inst *arc = client_data_ptr;
    int32_t r = (*arc->env)->CallIntMethod(arc->env, arc->file, arc->readID, arc->jbr);
    (*arc->env)->GetByteArrayRegion(arc->env, arc->jbr, 0, r, arc->read_buffer);
    *buff = arc->read_buffer;
    return r;
}

int ins_close(struct archive *a, void *client_data) {
    archive_inst *arc = client_data;
    JNIEnv *env = arc->env;
    jmethodID rewind = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, arc->file), "rewind",
                                           "()V");
    (*arc->env)->CallVoidMethod(arc->env, arc->file, rewind);
    return ARCHIVE_OK;
}

la_int64_t ins_seek(struct archive *a, void *client_data, la_int64_t offset, int whence) {
    archive_inst *arc = client_data;
    jlong ret = (*arc->env)->CallLongMethod(arc->env, arc->file, arc->seekID, offset, whence);
    return ret;
}

la_int64_t ins_skip(struct archive *a, void *client_data, la_int64_t request) {
    archive_inst *arc = client_data;
    jlong ret = (*arc->env)->CallLongMethod(arc->env, arc->file, arc->skipID, request);
    return ret;
}

static archive_inst *archive_create_inst(JNIEnv *env, jobject file) {
    archive_inst *inst = malloc(sizeof(archive_inst));
    inst->archive = archive_read_new();
    archive_read_set_seek_callback(inst->archive, ins_seek);
    archive_read_set_skip_callback(inst->archive, ins_skip);
    archive_read_support_format_all(inst->archive);
    archive_read_support_filter_all(inst->archive);
    inst->env = env;
    inst->file = file;
    inst->readID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, file), "read", "([B)I");
    inst->seekID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, file), "seek", "(JI)J");
    inst->skipID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, file), "skip", "(J)J");
    inst->jbr = (*env)->NewByteArray(env, BLOCK_SIZE);
    inst->read_buffer = malloc(BLOCK_SIZE);
    inst->output_buffer = malloc(BLOCK_SIZE);
    return inst;
}

static void archive_destroy_inst(archive_inst *inst) {
    JNIEnv *env = inst->env;
    archive_read_free(inst->archive);
    (*env)->DeleteLocalRef(env, inst->jbr);
    free(inst->read_buffer);
    free(inst->output_buffer);
    free(inst);
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_openArchive(JNIEnv *env, jobject thiz, jobject osf) {
    archive_inst *arc = archive_create_inst(env, osf);
    long r = archive_read_open(arc->archive, arc, NULL, ins_read, ins_close);
    if (r) {
        r = 0;
        LOGE("%s%s", "Archive open failed:", archive_error_string(arc->archive));
    } else {
        r = archive_list_all_entries(arc);
        LOGI("%s%ld%s", "Found ", r, " image entries in archive");
    }
    archive_read_close(arc->archive);
    archive_destroy_inst(arc);
    return r;
}

JNIEXPORT jint JNICALL
Java_com_hippo_UriArchiveAccessor_extracttoOutputStream(JNIEnv *env, jobject thiz, jobject osf,
                                                        jint index, jobject os) {
    int count = 0;
    struct archive_entry *entry;
    archive_inst *arc = archive_create_inst(env, osf);
    la_ssize_t size;
    jmethodID writeID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "write", "([B)V");
    jmethodID flushID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "flush", "()V");
    jmethodID closeID = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, os), "close", "()V");
    archive_read_open(arc->archive, arc, NULL, ins_read, ins_close);
    while (archive_read_next_header(arc->archive, &entry) == ARCHIVE_OK) {
        if (!filename_is_playable_file(archive_entry_pathname(entry)))
            continue;
        if (count++ == index) {
            for (;;) {
                size = archive_read_data(arc->archive, arc->output_buffer, BLOCK_SIZE);
                if (size < 0) {
                    LOGE("%s%s", "Archive read failed:", archive_error_string(arc->archive));
                    goto free;
                }
                (*arc->env)->SetByteArrayRegion(env, arc->jbr, 0, size, arc->output_buffer);
                (*arc->env)->CallVoidMethod(env, os, writeID, arc->jbr);
                (*arc->env)->CallVoidMethod(env, os, flushID);
                if (size < BLOCK_SIZE)
                    break;
            }
            break;
        }
    }
    free:
    (*arc->env)->CallVoidMethod(env, os, closeID);
    archive_read_close(arc->archive);
    archive_destroy_inst(arc);
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_UriArchiveAccessor_compressDirToOutStream(JNIEnv *env, jclass clazz, jstring dirname,
                                                         jint format, jobject os) {
    return JNI_FALSE;
}