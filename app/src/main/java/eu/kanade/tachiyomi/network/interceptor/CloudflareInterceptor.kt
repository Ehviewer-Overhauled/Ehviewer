package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.exception.EhException
import eu.kanade.tachiyomi.util.system.logcat
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(context: Context) : WebViewInterceptor(context) {
    private val executor = ContextCompat.getMainExecutor(context)

    override fun shouldIntercept(response: Response): Boolean {
        // Check if Cloudflare anti-bot is on
        return response.code in ERROR_CODES && response.header("Server") in SERVER_CHECK
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
        // we don't crash the entire app
        return runCatching {
            // TODO: Remove logging when it's stable
            logcat { "Response code: ${response.code}" }
            logcat { "Response body: ${response.body.string()}" }
            response.close()
            EhCookieStore.deleteCookie(request.url, COOKIE_NAME)
            resolveWithWebView(request)

            chain.proceed(request)
        }.getOrElse { throw IOException(it) }
    }

    private fun resolveWithWebView(originalRequest: Request) {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webview: WebView? = null

        var challengeFound = false
        var cloudflareBypassed = false

        val origRequestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)

        executor.execute {
            webview = createWebView(originalRequest)

            webview?.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return EhCookieStore.get(origRequestUrl.toHttpUrl())
                            .any { it.name == COOKIE_NAME }
                    }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

                    if (url == origRequestUrl && !challengeFound) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame && error.errorCode in ERROR_CODES) {
                        // Found the Cloudflare challenge page.
                        challengeFound = true
                    } else {
                        // Unlock thread, the challenge wasn't found.
                        latch.countDown()
                    }
                }
            }

            webview?.loadUrl(origRequestUrl, headers)
        }

        latch.awaitFor30Seconds()

        executor.execute {
            webview?.run {
                stopLoading()
                destroy()
            }
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) throw EhException("Failed to bypass Cloudflare")
    }
}

private val ERROR_CODES = intArrayOf(403, 503)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
private const val COOKIE_NAME = "cf_clearance"
