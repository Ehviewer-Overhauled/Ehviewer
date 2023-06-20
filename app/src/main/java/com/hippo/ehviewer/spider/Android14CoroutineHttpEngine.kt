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
    lateinit var consumer: (ByteBuffer) -> Unit
    lateinit var onResponse: CronetRequest.(UrlResponseInfo) -> Unit
    lateinit var request: UrlRequest
    lateinit var daemonCont: Continuation<Boolean>
    lateinit var readerCont: Continuation<Long>
    val callback = object : UrlRequest.Callback() {
        override fun onRedirectReceived(req: UrlRequest, info: UrlResponseInfo, url: String) {
            // No-op
        }

        override fun onResponseStarted(req: UrlRequest, info: UrlResponseInfo) {
            onResponse(info)
        }

        override fun onReadCompleted(req: UrlRequest, info: UrlResponseInfo, data: ByteBuffer) {
            data.flip()
            consumer(data)
            buffer.clear()
            request.read(buffer)
        }

        override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
            val length = info.receivedByteCount
            readerCont.resume(length)
            daemonCont.resume(true)
        }

        override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, e: CronetException) {
            daemonCont.resumeWithException(e)
        }

        override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
            // No-op
        }
    }

    val buffer = pool.borrow()

    override fun close() = pool.recycle(buffer)
}

inline fun cronetRequest(url: String, conf: UrlRequest.Builder.() -> Unit) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, callback, cronetHttpClientExecutor).apply(conf).build()
}

suspend infix fun CronetRequest.awaitBodyFully(consumer: (ByteBuffer) -> Unit) = run {
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
