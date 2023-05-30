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

import android.util.Log
import com.hippo.ehviewer.EhApplication.Companion.nonCacheOkHttpClient
import com.hippo.ehviewer.EhApplication.Companion.okHttpClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.executeAsync
import org.json.JSONArray
import org.json.JSONObject
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

inline fun ehRequest(url: String, referer: String? = null, origin: String? = null, builder: Request.Builder.() -> Unit = {}) = Request.Builder().url(url).apply {
    addHeader("User-Agent", CHROME_USER_AGENT)
    addHeader("Accept", CHROME_ACCEPT)
    addHeader("Accept-Language", CHROME_ACCEPT_LANGUAGE)
    referer?.let { addHeader("Referer", it) }
    origin?.let { addHeader("Origin", it) }
}.apply(builder).build()

inline fun Request.Builder.formBody(builder: FormBody.Builder.() -> Unit) = post(FormBody.Builder().apply(builder).build())

inline fun Request.Builder.multipartBody(builder: MultipartBody.Builder.() -> Unit) = post(MultipartBody.Builder().apply(builder).build())

val MEDIA_TYPE_JSON: MediaType = "application/json; charset=utf-8".toMediaType()

inline fun JSONObject.array(name: String, builder: JSONArray.() -> Unit): JSONObject = put(name, JSONArray().apply(builder))

fun jsonArrayOf(vararg element: Any?) = JSONArray().apply { element.forEach { put(it) } } // Should ensure it is inlined

inline fun Request.Builder.jsonBody(builder: JSONObject.() -> Unit) = post(JSONObject().apply(builder).toString().toRequestBody(MEDIA_TYPE_JSON))

const val TAG_CALL = "CancellableCallReadingScope"

// This scoped response reading scope suspend function aimed at cancelling the call immediately once coroutine is cancelled.
// i.e., bind a call with a suspended coroutine, and launch another coroutine to execute reading
// Once origin coroutine is cancelled, then call cancelled, socket is closed, the reading coroutine will encounter [IOException]
// Since Okio is not suspendable/cancellable, also okhttp, [executeAsync] only make [Request -> Response] cancellable, reading a Response is still not cancellable
// Considering interrupting is not safe and performant, See https://github.com/Kotlin/kotlinx.coroutines/issues/3551#issuecomment-1353245978, we have such [usingCancellable]
// TODO: Remove logging after this function stable
suspend inline fun <R> Call.usingCancellable(crossinline block: Response.() -> R): R = executeAsync().use {
    val call = this
    Log.d(TAG_CALL, "Got response of call$call")
    coroutineScope {
        suspendCancellableCoroutine<R> { cont ->
            launch {
                val r = runCatching {
                    Log.d(TAG_CALL, "Reading response of call$call")
                    block(it)
                }
                if (!isCanceled() && cont.isActive) {
                    r.exceptionOrNull()?.let {
                        Log.e(TAG_CALL, "Reading response of call$call failed!")
                        cont.resumeWithException(it)
                    }
                    r.getOrNull()?.let {
                        Log.d(TAG_CALL, "Reading response of call$call succeed!")
                        cont.resume(it)
                    }
                } else if (isCanceled() && cont.isActive) {
                    Log.e(TAG_CALL, "call$call cancelled but coroutine is Active!")
                } else if (!isCanceled() && !cont.isActive) {
                    Log.e(TAG_CALL, "call$call not cancelled but coroutine is Dead!")
                } else {
                    Log.d(TAG_CALL, "Reading response of call$call cancelled")
                }
            }
            cont.invokeOnCancellation {
                Log.d(TAG_CALL, "Cancelling call$call")
                cancel()
            }
        }
    }.apply {
        Log.d(TAG_CALL, "Reading response of call$call finished!")
    }
}

suspend inline fun <R> Request.execute(block: Response.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return okHttpClient.newCall(this).executeAsync().use(block)
}

suspend inline fun <R> Request.executeNonCache(crossinline block: Response.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return nonCacheOkHttpClient.newCall(this).usingCancellable(block)
}

const val CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36"
const val CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
const val CHROME_ACCEPT_LANGUAGE = "en-US,en;q=0.5"
