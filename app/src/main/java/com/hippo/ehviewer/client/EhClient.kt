/*
 * Copyright 2016 Hippo Seven
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
package com.hippo.ehviewer.client

import com.hippo.ehviewer.EhApplication.Companion.okHttpClient
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import java.io.File

object EhClient {
    internal fun enqueue(request: EhRequest, scope: CoroutineScope) {
        check(!request.isActive) // Abort if attempt to execute an active request
        request.job = scope.launchIO {
            val callback: Callback<Any?>? = request.callback
            try {
                val result: Any? = execute(request.method, request.args)
                withUIContext { callback?.onSuccess(result) }
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e // Don't catch coroutine CancellationException
                e.printStackTrace()
                withUIContext { callback?.onFailure(e) }
            }
        }
    }

    suspend fun execute(method: Int, vararg params: Any?): Any? {
        return when (method) {
            METHOD_SIGN_IN -> EhEngine.signIn(
                mOkHttpClient,
                params[0] as String,
                params[1] as String
            )

            METHOD_GET_GALLERY_LIST -> EhEngine.getGalleryList(
                mOkHttpClient,
                params[0] as String
            )

            METHOD_GET_GALLERY_DETAIL -> EhEngine.getGalleryDetail(
                mOkHttpClient,
                params[0] as String?
            )

            METHOD_GET_PREVIEW_SET -> EhEngine.getPreviewSet(
                mOkHttpClient,
                params[0] as String?
            )

            METHOD_GET_RATE_GALLERY -> EhEngine.rateGallery(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as Long,
                params[3] as String?,
                params[4] as Float
            )

            METHOD_GET_COMMENT_GALLERY -> EhEngine.commentGallery(
                mOkHttpClient,
                params[0] as String?,
                params[1] as String,
                params[2] as String?
            )

            METHOD_GET_GALLERY_TOKEN -> EhEngine.getGalleryToken(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as Int
            )

            METHOD_GET_FAVORITES -> EhEngine.getFavorites(
                mOkHttpClient,
                params[0] as String
            )

            METHOD_ADD_FAVORITES -> EhEngine.addFavorites(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as Int,
                params[3] as String?
            )

            METHOD_ADD_FAVORITES_RANGE -> @Suppress("UNCHECKED_CAST") EhEngine.addFavoritesRange(
                mOkHttpClient,
                params[0] as LongArray,
                params[1] as Array<String?>,
                params[2] as Int
            )

            METHOD_MODIFY_FAVORITES -> EhEngine.modifyFavorites(
                mOkHttpClient,
                params[0] as String,
                params[1] as LongArray,
                params[2] as Int
            )

            METHOD_GET_TORRENT_LIST -> EhEngine.getTorrentList(
                mOkHttpClient,
                params[0] as String?,
                params[1] as Long,
                params[2] as String?
            )

            METHOD_GET_PROFILE -> EhEngine.getProfile(mOkHttpClient)
            METHOD_VOTE_COMMENT -> EhEngine.voteComment(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as Long,
                params[3] as String?,
                params[4] as Long,
                params[5] as Int
            )

            METHOD_IMAGE_SEARCH -> EhEngine.imageSearch(
                mOkHttpClient,
                params[0] as File,
                params[1] as Boolean,
                params[2] as Boolean,
                params[3] as Boolean
            )

            METHOD_ARCHIVE_LIST -> EhEngine.getArchiveList(
                mOkHttpClient,
                params[0] as String?,
                params[1] as Long,
                params[2] as String?
            )

            METHOD_DOWNLOAD_ARCHIVE -> EhEngine.downloadArchive(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as String?,
                params[3] as String?,
                params[4] as Boolean
            )

            METHOD_VOTE_TAG -> EhEngine.voteTag(
                mOkHttpClient,
                params[0] as Long,
                params[1] as String?,
                params[2] as Long,
                params[3] as String?,
                params[4] as String?,
                params[5] as Int
            )

            METHOD_GET_UCONFIG -> EhEngine.getUConfig(mOkHttpClient)
            else -> throw IllegalStateException("Can't detect method $method")
        }
    }

    interface Callback<E> {
        fun onSuccess(result: E)
        fun onFailure(e: Exception)
        fun onCancel()
    }

    const val METHOD_SIGN_IN = 0
    const val METHOD_GET_GALLERY_LIST = 1
    const val METHOD_GET_GALLERY_DETAIL = 3
    const val METHOD_GET_PREVIEW_SET = 4
    const val METHOD_GET_RATE_GALLERY = 5
    const val METHOD_GET_COMMENT_GALLERY = 6
    const val METHOD_GET_GALLERY_TOKEN = 7
    const val METHOD_GET_FAVORITES = 8
    const val METHOD_ADD_FAVORITES = 9
    const val METHOD_ADD_FAVORITES_RANGE = 10
    const val METHOD_MODIFY_FAVORITES = 11
    const val METHOD_GET_TORRENT_LIST = 12
    const val METHOD_GET_PROFILE = 14
    const val METHOD_VOTE_COMMENT = 15
    const val METHOD_IMAGE_SEARCH = 16
    const val METHOD_ARCHIVE_LIST = 17
    const val METHOD_DOWNLOAD_ARCHIVE = 18
    const val METHOD_VOTE_TAG = 19
    const val METHOD_GET_UCONFIG = 20
    private val mOkHttpClient = okHttpClient
}