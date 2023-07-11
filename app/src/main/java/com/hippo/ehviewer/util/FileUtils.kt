/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.util

import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FileUtils {
    private val FORBIDDEN_FILENAME_CHARACTERS = charArrayOf(
        '\\',
        '/',
        ':',
        '*',
        '?',
        '"',
        '<',
        '>',
        '|',
    )

    fun ensureDirectory(file: File?) =
        file?.let { if (it.exists()) it.isDirectory else it.mkdirs() } ?: false

    /**
     * Convert byte to human readable string.<br></br>
     * http://stackoverflow.com/questions/3758606/
     *
     * @param bytes the bytes to convert
     * @param si    si units
     * @return the human readable string
     */
    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        return String.format(
            Locale.US,
            "%.1f %sB",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre,
        )
    }

    /**
     * Try to delete file, dir and it's children
     *
     * @param file the file to delete
     * The dir to deleted
     */
    fun delete(file: File?): Boolean {
        file ?: return false
        return deleteContent(file) and file.delete()
    }

    fun deleteContent(file: File?): Boolean {
        file ?: return false
        var success = true
        file.listFiles()?.forEach {
            success = success and delete(it)
        }
        return success
    }

    fun sanitizeFilename(rawFilename: String): String {
        // Remove forbidden_filename_characters
        var filename = rawFilename
        filename = filename.filterNot { FORBIDDEN_FILENAME_CHARACTERS.contains(it) }

        // Ensure utf-8 byte count <= 255
        var byteCount = 0
        var length = 0
        val len = filename.length
        while (length < len) {
            val ch = filename[length]
            if (ch.code <= 0x7F) {
                byteCount++
            } else if (ch.code <= 0x7FF) {
                byteCount += 2
            } else if (Character.isHighSurrogate(ch)) {
                byteCount += 4
                ++length
            } else {
                byteCount += 3
            }
            // Meet max byte count
            if (byteCount > 255) {
                filename = filename.substring(0, length)
                break
            }
            length++
        }

        // Trim
        return filename.trim()
    }

    /**
     * Get extension from filename
     *
     * @param filename the complete filename
     * @return null for can't find extension
     */
    fun getExtensionFromFilename(filename: String?) =
        filename?.substringAfterLast('.', "").takeUnless { it.isNullOrEmpty() }

    /**
     * Get name from filename
     *
     * @param filename the complete filename
     * @return null for start with . dot
     */
    fun getNameFromFilename(filename: String?) =
        filename?.substringBeforeLast('.').takeUnless { it.isNullOrEmpty() }

    /**
     * Create a temp file, you need to delete it by you self.
     *
     * @param parent    The temp file's parent
     * @param extension The extension of temp file
     * @return The temp file or null
     */
    fun createTempFile(parent: File?, extension: String?): File? {
        parent ?: return null
        val now = System.currentTimeMillis()
        for (i in 0..99) {
            var filename = (now + i).toString()
            extension?.let {
                filename = "$filename.$it"
            }
            val tempFile = File(parent, filename)
            if (!tempFile.exists()) {
                return tempFile
            }
        }

        // Unbelievable
        return null
    }
}
