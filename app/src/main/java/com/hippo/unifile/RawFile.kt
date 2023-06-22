/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.hippo.unifile

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

internal class RawFile(parent: UniFile?, var mFile: File) : UniFile(parent) {
    override fun createFile(displayName: String): UniFile? {
        val target = File(mFile, displayName)
        return if (target.exists()) {
            if (target.isFile) {
                RawFile(this, target)
            } else {
                null
            }
        } else {
            runCatching {
                FileOutputStream(target).use {}
                RawFile(this, target)
            }.getOrElse {
                Log.w(TAG, "Failed to createFile $displayName: $it")
                null
            }
        }
    }

    override fun createDirectory(displayName: String): UniFile? {
        val target = File(mFile, displayName)
        return if (target.isDirectory || target.mkdirs()) {
            RawFile(this, target)
        } else {
            null
        }
    }

    override val uri: Uri
        get() = Uri.fromFile(mFile)
    override val name: String
        get() = mFile.name
    override val type: String?
        get() = if (mFile.isDirectory) {
            null
        } else {
            getTypeForName(mFile.name)
        }
    override val isDirectory: Boolean
        get() = mFile.isDirectory
    override val isFile: Boolean
        get() = mFile.isFile

    override fun lastModified(): Long {
        return mFile.lastModified()
    }

    override fun length(): Long {
        return mFile.length()
    }

    override fun canRead(): Boolean {
        return mFile.canRead()
    }

    override fun canWrite(): Boolean {
        return mFile.canWrite()
    }

    override fun ensureDir(): Boolean {
        return mFile.isDirectory || mFile.mkdirs()
    }

    override fun ensureFile(): Boolean {
        return if (mFile.exists()) {
            mFile.isFile
        } else {
            runCatching {
                FileOutputStream(mFile).use {}
                true
            }.getOrDefault(false)
        }
    }

    override fun subFile(displayName: String): UniFile {
        return RawFile(this, File(mFile, displayName))
    }

    override fun delete(): Boolean {
        deleteContents(mFile)
        return mFile.delete()
    }

    override fun exists(): Boolean {
        return mFile.exists()
    }

    override fun listFiles(): Array<UniFile>? {
        val files = mFile.listFiles() ?: return null
        return files.map { RawFile(this, it) }.toTypedArray()
    }

    override fun listFiles(filter: FilenameFilter?): Array<UniFile>? {
        if (filter == null) {
            return listFiles()
        }
        val files = mFile.listFiles() ?: return null
        val results = ArrayList<UniFile>()
        for (file in files) {
            if (filter.accept(this, file.name)) {
                results.add(RawFile(this, file))
            }
        }
        return results.toTypedArray<UniFile>()
    }

    override fun findFile(displayName: String): UniFile? {
        val child = File(mFile, displayName)
        return if (child.exists()) RawFile(this, child) else null
    }

    override fun renameTo(displayName: String): Boolean {
        val target = File(mFile.parentFile, displayName)
        return if (mFile.renameTo(target)) {
            mFile = target
            true
        } else {
            false
        }
    }

    override val imageSource: ImageDecoder.Source
        get() = ImageDecoder.createSource(mFile)

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor {
        val md = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(mFile, md)
            ?: throw IOException("Can't open ParcelFileDescriptor")
    }

    companion object {
        private val TAG = RawFile::class.java.simpleName
        private fun getTypeForName(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).lowercase(Locale.getDefault())
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) {
                    return mime
                }
            }
            return "application/octet-stream"
        }

        private fun deleteContents(dir: File): Boolean {
            val files = dir.listFiles()
            var success = true
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        success = success and deleteContents(file)
                    }
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete $file")
                        success = false
                    }
                }
            }
            return success
        }
    }
}
