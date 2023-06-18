@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.spider

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import com.hippo.ehviewer.EhApplication
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val cronetHttpClient = HttpEngine.Builder(appCtx).apply {
    setEnableHttp2(true)
    setEnableBrotli(true)
    setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISABLED, 0)
    setEnableQuic(true)
}.build()

val cronetHttpClientExecutor = EhApplication.nonCacheOkHttpClient.dispatcher.executorService

class CronetRequest {
    lateinit var mConsumer: (UrlResponseInfo, ByteBuffer) -> Unit
    lateinit var onResponse: CronetRequest.(UrlResponseInfo) -> Unit
    lateinit var request: UrlRequest
    lateinit var daemonCont: Continuation<Boolean>
    lateinit var readerCont: Continuation<Unit>
    val callback = object : UrlRequest.Callback {
        override fun onRedirectReceived(p0: UrlRequest, p1: UrlResponseInfo, p2: String) {
            // No-op
        }

        override fun onResponseStarted(p0: UrlRequest, p1: UrlResponseInfo) {
            onResponse(p1)
        }

        override fun onReadCompleted(p0: UrlRequest, p1: UrlResponseInfo, p2: ByteBuffer) {
            p2.flip()
            mConsumer(p1, p2)
            cronetHttpClientExecutor.execute {
                buffer.flip()
                request.read(buffer)
            }
        }

        override fun onSucceeded(p0: UrlRequest, p1: UrlResponseInfo) {
            val length = p1.receivedByteCount
            // TODO: validate body length
            readerCont.resume(Unit)
            daemonCont.resume(true)
        }

        override fun onFailed(p0: UrlRequest, p1: UrlResponseInfo?, p2: HttpException) {
            daemonCont.resumeWithException(p2)
        }

        override fun onCanceled(p0: UrlRequest, p1: UrlResponseInfo?) {
            // No-op
        }
    }

    // TODO: Pool it
    val buffer: ByteBuffer = ByteBuffer.allocateDirect(4096)
}

inline fun cronetRequest(url: String, conf: UrlRequest.Builder.() -> Unit) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, cronetHttpClientExecutor, callback).apply(conf).build()
}

suspend infix fun CronetRequest.consumeBody(callback: (UrlResponseInfo, ByteBuffer) -> Unit) = apply {
    mConsumer = callback
    suspendCancellableCoroutine { cont ->
        readerCont = cont
        request.read(buffer)
    }
}

suspend inline infix fun CronetRequest.execute(noinline callback: suspend CronetRequest.(UrlResponseInfo) -> Unit): Boolean {
    return coroutineScope {
        onResponse = {
            launch {
                callback(it)
            }
        }
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { request.cancel() }
            daemonCont = cont
            request.start()
        }
    }
}
