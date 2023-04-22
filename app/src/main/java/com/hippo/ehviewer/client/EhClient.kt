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

import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

object EhClient {
    internal fun enqueue(request: EhRequest, scope: CoroutineScope) {
        check(!request.isActive) // Abort if attempt to execute an active request
        request.job = scope.launchIO {
            val callback: Callback<Any?>? = request.callback
            try {
                val result: Any? = request.run {
                    if (args == null) {
                        execute(method)
                    } else {
                        execute(method, *args!!)
                    }
                }
                withUIContext { callback?.onSuccess(result) }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e // Don't catch coroutine CancellationException
                }
                e.printStackTrace()
                withUIContext { callback?.onFailure(e) }
            }
        }
    }

    suspend fun execute(method: Int, vararg params: Any?): Any? {
        return when (method) {
            METHOD_GET_COMMENT_GALLERY -> EhEngine.commentGallery(
                params[0] as String?,
                params[1] as String,
                params[2] as String?,
            )

            METHOD_GET_FAVORITES -> EhEngine.getFavorites(
                params[0] as String,
            )

            METHOD_ADD_FAVORITES_RANGE ->
                @Suppress("UNCHECKED_CAST")
                EhEngine.addFavoritesRange(
                    params[0] as LongArray,
                    params[1] as Array<String?>,
                    params[2] as Int,
                )

            METHOD_MODIFY_FAVORITES -> EhEngine.modifyFavorites(
                params[0] as String,
                params[1] as LongArray,
                params[2] as Int,
            )

            METHOD_GET_TORRENT_LIST -> EhEngine.getTorrentList(
                params[0] as String,
                params[1] as Long,
                params[2] as String?,
            )

            METHOD_ARCHIVE_LIST -> EhEngine.getArchiveList(
                params[0] as String,
                params[1] as Long,
                params[2] as String?,
            )

            else -> throw IllegalStateException("Can't detect method $method")
        }
    }

    interface Callback<E> {
        fun onSuccess(result: E)
        fun onFailure(e: Exception)
        fun onCancel()
    }

    const val METHOD_GET_COMMENT_GALLERY = 6
    const val METHOD_GET_FAVORITES = 8
    const val METHOD_ADD_FAVORITES_RANGE = 10
    const val METHOD_MODIFY_FAVORITES = 11
    const val METHOD_GET_TORRENT_LIST = 12
    const val METHOD_ARCHIVE_LIST = 17
}
