/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract

internal object DocumentsContractApi19 {
    fun isDocumentUri(context: Context, self: Uri): Boolean {
        return DocumentsContract.isDocumentUri(context, self)
    }

    fun getName(context: Context, self: Uri): String? {
        return Contracts.queryForString(
            context,
            self,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            null,
        )
    }

    private fun getRawType(context: Context, self: Uri): String? {
        return Contracts.queryForString(
            context,
            self,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            null,
        )
    }

    fun getType(context: Context, self: Uri): String? {
        return getRawType(context, self).takeUnless { it == DocumentsContract.Document.MIME_TYPE_DIR }
    }

    fun isDirectory(context: Context, self: Uri): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR == getRawType(context, self)
    }

    fun isFile(context: Context, self: Uri): Boolean {
        val type = getRawType(context, self)
        return !(DocumentsContract.Document.MIME_TYPE_DIR == type || type.isNullOrEmpty())
    }

    fun lastModified(context: Context, self: Uri): Long {
        return Contracts.queryForLong(
            context,
            self,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            -1L,
        )
    }

    fun length(context: Context, self: Uri): Long {
        return Contracts.queryForLong(context, self, DocumentsContract.Document.COLUMN_SIZE, -1L)
    }

    fun canRead(context: Context, self: Uri): Boolean {
        // Ignore if grant doesn't allow read
        return if (
            context.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else {
            // Ignore documents without MIME
            !getRawType(context, self).isNullOrEmpty()
        }
    }

    fun canWrite(context: Context, self: Uri): Boolean {
        // Ignore if grant doesn't allow write
        if (context.checkCallingOrSelfUriPermission(self, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val type = getRawType(context, self)
        val flags = Contracts.queryForInt(context, self, DocumentsContract.Document.COLUMN_FLAGS, 0)

        // Ignore documents without MIME
        if (type.isNullOrEmpty()) {
            return false
        }

        // Deletable documents considered writable
        if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
            return true
        }

        // Writable normal files considered writable
        return if (DocumentsContract.Document.MIME_TYPE_DIR == type && flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0) {
            // Directories that allow create considered writable
            true
        } else {
            flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
        }
    }

    fun delete(context: Context, self: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, self)
        } catch (e: Throwable) {
            Utils.throwIfFatal(e)
            false
        }
    }

    fun exists(context: Context, self: Uri): Boolean {
        return runCatching {
            context.contentResolver.query(
                self,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            ).use { null != it && it.count > 0 }
        }.getOrElse {
            Utils.throwIfFatal(it)
            false
        }
    }
}
