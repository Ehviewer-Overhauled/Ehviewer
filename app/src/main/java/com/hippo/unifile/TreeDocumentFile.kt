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

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap

internal class TreeDocumentFile : UniFile {
    private val mContext: Context
    override var uri: Uri
        private set
    private var mFilename: String? = null

    constructor(parent: UniFile?, context: Context, uri: Uri) : super(parent) {
        mContext = context.applicationContext
        this.uri = uri
    }

    private constructor(parent: UniFile, context: Context, uri: Uri, filename: String?) : super(
        parent,
    ) {
        mContext = context.applicationContext
        this.uri = uri
        mFilename = filename
    }

    override fun createFile(displayName: String): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            if (child.isFile) {
                child
            } else {
                Log.w(
                    TAG,
                    "Try to create file $displayName, but it is not file",
                )
                null
            }
        } else {
            val index = displayName.lastIndexOf('.')
            if (index > 0) {
                val name = displayName.substring(0, index)
                val extension = displayName.substring(index + 1)
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (!mimeType.isNullOrEmpty()) {
                    val result = DocumentsContractApi21.createFile(mContext, uri, mimeType, name)
                    return if (result != null) {
                        TreeDocumentFile(
                            this,
                            mContext,
                            result,
                            displayName,
                        )
                    } else {
                        null
                    }
                }
            }

            // Not dot in displayName or dot is the first char or can't get MimeType
            val result = DocumentsContractApi21.createFile(
                mContext,
                uri,
                "application/octet-stream",
                displayName,
            )
            if (result != null) {
                TreeDocumentFile(
                    this,
                    mContext,
                    result,
                    displayName,
                )
            } else {
                null
            }
        }
    }

    override fun createDirectory(displayName: String): UniFile? {
        val child = findFile(displayName)
        return if (child != null) {
            if (child.isDirectory) {
                child
            } else {
                null
            }
        } else {
            val result = DocumentsContractApi21.createDirectory(mContext, uri, displayName)
            if (result != null) {
                TreeDocumentFile(
                    this,
                    mContext,
                    result,
                    displayName,
                )
            } else {
                null
            }
        }
    }

    override val name: String?
        get() = DocumentsContractApi19.getName(mContext, uri)
    override val type: String?
        get() = DocumentsContractApi19.getType(mContext, uri)
    override val isDirectory: Boolean
        get() = DocumentsContractApi19.isDirectory(mContext, uri)
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(mContext, uri)

    override fun lastModified(): Long {
        return DocumentsContractApi19.lastModified(mContext, uri)
    }

    override fun length(): Long {
        return DocumentsContractApi19.length(mContext, uri)
    }

    override fun canRead(): Boolean {
        return DocumentsContractApi19.canRead(mContext, uri)
    }

    override fun canWrite(): Boolean {
        return DocumentsContractApi19.canWrite(mContext, uri)
    }

    override fun ensureDir(): Boolean {
        if (isDirectory) {
            return true
        } else if (isFile) {
            return false
        }
        val parent = parentFile
        return if (parent != null && parent.ensureDir() && mFilename != null) {
            parent.createDirectory(mFilename!!) != null
        } else {
            false
        }
    }

    override fun ensureFile(): Boolean {
        if (isFile) {
            return true
        } else if (isDirectory) {
            return false
        }
        val parent = parentFile
        return if (parent != null && parent.ensureDir() && mFilename != null) {
            parent.createFile(mFilename!!) != null
        } else {
            false
        }
    }

    override fun subFile(displayName: String): UniFile {
        val childUri = DocumentsContractApi21.buildChildUri(uri, displayName)
        return TreeDocumentFile(this, mContext, childUri, displayName)
    }

    override fun delete(): Boolean {
        return DocumentsContractApi19.delete(mContext, uri)
    }

    override fun exists(): Boolean {
        return DocumentsContractApi19.exists(mContext, uri)
    }

    private fun getFilenameForUri(uri: Uri): String? {
        val path = uri.path
        if (path != null) {
            val index = path.lastIndexOf('/')
            if (index >= 0) {
                return path.substring(index + 1)
            }
        }
        return null
    }

    override fun listFiles(): Array<UniFile> {
        val result = DocumentsContractApi21.listFiles(mContext, uri)
        return result.map { TreeDocumentFile(this, mContext, it, getFilenameForUri(it)) }.toTypedArray()
    }

    override fun listFiles(filter: FilenameFilter?): Array<UniFile> {
        if (filter == null) {
            return listFiles()
        }
        val result = DocumentsContractApi21.listFiles(mContext, uri)
        val results = ArrayList<UniFile>()
        for (uri in result) {
            val name = getFilenameForUri(uri)
            if (filter.accept(this, name)) {
                results.add(TreeDocumentFile(this, mContext, uri, name))
            }
        }
        return results.toTypedArray<UniFile>()
    }

    override fun findFile(displayName: String): UniFile? {
        val childUri = DocumentsContractApi21.buildChildUri(uri, displayName)
        return if (DocumentsContractApi19.exists(mContext, childUri)) {
            TreeDocumentFile(
                this,
                mContext,
                childUri,
                displayName,
            )
        } else {
            null
        }
    }

    override fun renameTo(displayName: String): Boolean {
        val result = DocumentsContractApi21.renameTo(mContext, uri, displayName)
        return if (result != null) {
            uri = result
            true
        } else {
            false
        }
    }

    override val imageSource: ImageDecoder.Source
        get() = Contracts.getImageSource(mContext, uri)

    override fun openFileDescriptor(mode: String): ParcelFileDescriptor {
        return Contracts.openFileDescriptor(mContext, uri, mode)
    }

    companion object {
        private val TAG = TreeDocumentFile::class.java.simpleName
    }
}
