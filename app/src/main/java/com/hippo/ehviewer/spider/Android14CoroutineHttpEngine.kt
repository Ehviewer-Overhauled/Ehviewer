package com.hippo.ehviewer.spider

import com.hippo.ehviewer.EhApplication
import io.ktor.utils.io.pool.DirectByteBufferPool
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import splitties.init.appCtx
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val pool = DirectByteBufferPool(32)

val cronetHttpClient: CronetEngine = CronetEngine.Builder(appCtx).apply {
    enableHttp2(true)
    enableBrotli(true)
    enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISABLED, 0)
    enableQuic(true)
}.build()

val cronetHttpClientExecutor = EhApplication.nonCacheOkHttpClient.dispatcher.executorService

// TODO: Rewrite this to use android.net.http.HttpEngine and make it Android 14 only when released
class CronetRequest : AutoCloseable {
    lateinit var consumer: (UrlResponseInfo, ByteBuffer) -> Unit
    lateinit var onResponse: CronetRequest.(UrlResponseInfo) -> Unit
    lateinit var request: UrlRequest
    lateinit var daemonCont: Continuation<Boolean>
    lateinit var readerCont: Continuation<Long>
    val callback = object : UrlRequest.Callback() {
        override fun onRedirectReceived(p0: UrlRequest, p1: UrlResponseInfo, p2: String) {
            // No-op
        }

        override fun onResponseStarted(p0: UrlRequest, p1: UrlResponseInfo) {
            onResponse(p1)
        }

        override fun onReadCompleted(p0: UrlRequest, p1: UrlResponseInfo, p2: ByteBuffer) {
            p2.flip()
            consumer(p1, p2)
            buffer.clear()
            request.read(buffer)
        }

        override fun onSucceeded(p0: UrlRequest, p1: UrlResponseInfo) {
            val length = p1.receivedByteCount
            readerCont.resume(length)
            daemonCont.resume(true)
        }

        override fun onFailed(p0: UrlRequest, p1: UrlResponseInfo?, p2: CronetException) {
            daemonCont.resumeWithException(p2)
        }

        override fun onCanceled(p0: UrlRequest, p1: UrlResponseInfo?) {
        }
    }

    val buffer = pool.borrow()

    override fun close() = pool.recycle(buffer)
}

inline fun cronetRequest(url: String, conf: UrlRequest.Builder.() -> Unit) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, callback, cronetHttpClientExecutor).apply(conf).build()
}

suspend infix fun CronetRequest.awaitBodyFully(consumer: (UrlResponseInfo, ByteBuffer) -> Unit) = run {
    this.consumer = consumer
    suspendCancellableCoroutine { cont ->
        readerCont = cont
        request.read(buffer)
    }
}

suspend inline fun CronetRequest.execute(noinline onResponse: suspend CronetRequest.(UrlResponseInfo) -> Unit): Boolean {
    return use {
        coroutineScope {
            this@execute.onResponse = { launch { onResponse(it) } }
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { request.cancel() }
                daemonCont = cont
                request.start()
            }
        }
    }
}
