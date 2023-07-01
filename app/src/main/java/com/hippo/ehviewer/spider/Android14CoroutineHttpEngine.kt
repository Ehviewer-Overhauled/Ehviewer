package com.hippo.ehviewer.spider

import com.hippo.ehviewer.EhApplication
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.utils.io.pool.DirectByteBufferPool
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Path.Companion.toOkioPath
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import splitties.init.appCtx
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CronetRequest"
private val pool = DirectByteBufferPool(32)

val cronetHttpClient: CronetEngine = CronetEngine.Builder(appCtx).apply {
    enableBrotli(true)
    val cache = (appCtx.cacheDir.toOkioPath() / "http_cache").toFile().apply { mkdirs() }
    setStoragePath(cache.absolutePath)
    enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)
    addQuicHint("e-hentai.org", 443, 443)
    addQuicHint("forums.e-hentai.org", 443, 443)
    addQuicHint("exhentai.org", 443, 443)
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
            logcat(tag = TAG) { "Redirected to $url" }
            req.followRedirect()
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
