package com.hippo.ehviewer.cronet

import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.client.CHROME_ACCEPT
import com.hippo.ehviewer.client.CHROME_ACCEPT_LANGUAGE
import com.hippo.ehviewer.client.CHROME_USER_AGENT
import com.hippo.ehviewer.client.EhCookieStore
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.utils.io.pool.DirectByteBufferPool
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Path.Companion.toOkioPath
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import splitties.init.appCtx
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CronetRequest"
val pool = DirectByteBufferPool(32)

val cronetHttpClient: CronetEngine = CronetEngine.Builder(appCtx).apply {
    enableBrotli(true)
    val cache = (appCtx.cacheDir.toOkioPath() / "http_cache").toFile().apply { mkdirs() }
    setStoragePath(cache.absolutePath)
    enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)
    addQuicHint("e-hentai.org", 443, 443)
    addQuicHint("forums.e-hentai.org", 443, 443)
    addQuicHint("exhentai.org", 443, 443)
}.build()

val cronetHttpClientExecutor = EhApplication.baseOkHttpClient.dispatcher.executorService

// TODO: Rewrite this to use android.net.http.HttpEngine and make it Android 14 only when released
class CronetRequest {
    lateinit var consumer: (ByteBuffer) -> Unit
    lateinit var onResponse: CronetRequest.(UrlResponseInfo) -> Unit
    lateinit var request: UrlRequest
    lateinit var onError: (Throwable) -> Unit
    lateinit var readerCont: Continuation<Unit>
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
        }

        override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
            readerCont.resume(Unit)
        }

        override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, e: CronetException) {
            onError(e)
        }
    }
}

inline fun cronetRequest(url: String, referer: String? = null, conf: UrlRequest.Builder.() -> Unit = {}) = CronetRequest().apply {
    request = cronetHttpClient.newUrlRequestBuilder(url, callback, cronetHttpClientExecutor).apply {
        addHeader("Cookie", EhCookieStore.getCookieHeader(url.toHttpUrl()))
        addHeader("User-Agent", CHROME_USER_AGENT)
        addHeader("Accept", CHROME_ACCEPT)
        addHeader("Accept-Language", CHROME_ACCEPT_LANGUAGE)
        referer?.let { addHeader("Referer", it) }
    }.apply(conf).build()
}

suspend inline fun CronetRequest.awaitBodyFully(crossinline callback: (ByteBuffer) -> Unit) {
    val buffer = pool.borrow()
    return try {
        suspendCancellableCoroutine { cont ->
            consumer = {
                callback(it)
                buffer.clear()
                request.read(buffer)
            }
            onError = { readerCont.resumeWithException(it) }
            readerCont = cont
            request.read(buffer)
        }
    } finally {
        pool.recycle(buffer)
    }
}

suspend inline fun CronetRequest.copyToChannel(chan: FileChannel, crossinline listener: ((Int) -> Unit) = {}) = awaitBodyFully {
    val bytes = chan.write(it)
    listener(bytes)
}

suspend inline fun <R> CronetRequest.execute(crossinline callback: suspend CronetRequest.(UrlResponseInfo) -> R): R {
    contract {
        callsInPlace(callback, InvocationKind.EXACTLY_ONCE)
    }
    return coroutineScope {
        suspendCancellableCoroutine { cont ->
            onResponse = { launch { cont.resume(callback(it)) } }
            cont.invokeOnCancellation { request.cancel() }
            onError = { cont.resumeWithException(it) }
            request.start()
        }
    }
}
