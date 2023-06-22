/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.unifile

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException

internal class MediaFile(context: Context, override val uri: Uri) : UniFile(null) {
    private val mContext: Context

    init {
        mContext = context.applicationContext
    }

    override fun createFile(displayName: String): UniFile? {
        return null
    }

    override fun createDirectory(displayName: String): UniFile? {
        return null
    }

    override val name: String?
        get() = MediaContract.getName(mContext, uri)
    override val type: String?
        get() = MediaContract.getType(mContext, uri)
    override val isDirectory: Boolean
        get() = false
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(mContext, uri)

    override fun lastModified(): Long {
        return MediaContract.lastModified(mContext, uri)
    }

    override fun length(): Long {
        return MediaContract.length(mContext, uri)
    }

    override fun canRead(): Boolean {
        return isFile
    }

    override fun canWrite(): Boolean {
        try {
            val fd = openFileDescriptor("w")
            fd.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    override fun ensureDir(): Boolean {
        return false
    }

    override fun ensureFile(): Boolean {
        return isFile
    }

    override fun subFile(displayName: String): UniFile? {
        return null
    }

    override fun delete(): Boolean {
        return false
    }

    override fun exists(): Boolean {
        return isFile
    }

    override fun listFiles(): Array<UniFile>? {
        return null
    }

    override fun listFiles(filter: FilenameFilter?): Array<UniFile>? {
        return null
    }

    override fun findFile(displayName: String): UniFile? {
        return null
    }

    override fun renameTo(displayName: String): Boolean {
        return false
    }

    override val imageSource: ImageDecoder.Source
        get() = Contracts.getImageSource(mContext, uri)

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor {
        return Contracts.openFileDescriptor(mContext, uri, mode)
    }

    companion object {
        fun isMediaUri(context: Context, uri: Uri): Boolean {
            return null != MediaContract.getName(context, uri)
        }
    }
}
