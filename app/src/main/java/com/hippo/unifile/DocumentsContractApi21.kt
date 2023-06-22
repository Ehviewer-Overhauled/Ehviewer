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
import android.net.Uri
import android.provider.DocumentsContract

internal object DocumentsContractApi21 {
    private const val PATH_DOCUMENT = "document"
    private const val PATH_TREE = "tree"
    fun createFile(context: Context, self: Uri, mimeType: String, displayName: String): Uri? {
        return try {
            DocumentsContract.createDocument(
                context.contentResolver,
                self,
                mimeType,
                displayName,
            )
        } catch (e: Throwable) {
            Utils.throwIfFatal(e)
            null
        }
    }

    fun createDirectory(context: Context, self: Uri, displayName: String): Uri? {
        return createFile(context, self, DocumentsContract.Document.MIME_TYPE_DIR, displayName)
    }

    fun prepareTreeUri(treeUri: Uri?): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
    }

    fun getTreeDocumentPath(documentUri: Uri): String {
        val paths = documentUri.pathSegments
        if (paths.size >= 4 && PATH_TREE == paths[0] && PATH_DOCUMENT == paths[2]) {
            return paths[3]
        }
        throw IllegalArgumentException("Invalid URI: $documentUri")
    }

    fun buildChildUri(uri: Uri, displayName: String): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(
            uri,
            getTreeDocumentPath(uri) + "/" + displayName,
        )
    }

    fun listFiles(context: Context, self: Uri): Array<Uri> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            self,
            DocumentsContract.getDocumentId(self),
        )
        val results = ArrayList<Uri>()
        runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            ).use {
                if (null != it) {
                    while (it.moveToNext()) {
                        val documentId = it.getString(0)
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                            self,
                            documentId,
                        )
                        results.add(documentUri)
                    }
                }
            }
        }.onFailure {
            Utils.throwIfFatal(it)
        }
        return results.toTypedArray<Uri>()
    }

    fun renameTo(context: Context, self: Uri, displayName: String): Uri? {
        return try {
            DocumentsContract.renameDocument(context.contentResolver, self, displayName)
        } catch (e: Throwable) {
            Utils.throwIfFatal(e)
            null
        }
    }
}
