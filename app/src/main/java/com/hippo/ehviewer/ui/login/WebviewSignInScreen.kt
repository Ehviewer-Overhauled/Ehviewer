package com.hippo.ehviewer.ui.login

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.widget.DialogWebChromeClient
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebviewSignInScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    AndroidView(factory = {
        EhUtils.signOut()

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        CookieManager.getInstance().apply {
            flush()
            removeAllCookies(null)
            removeSessionCookies(null)
        }
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.run {
                builtInZoomControls = true
                displayZoomControls = true
                javaScriptEnabled = true
            }
            webViewClient = object : WebViewClient() {
                private var present = false
                fun parseCookies(url: HttpUrl?, cookieStrings: String?): List<Cookie> {
                    if (cookieStrings == null) {
                        return emptyList()
                    }
                    var cookies: MutableList<Cookie>? = null
                    val pieces =
                        cookieStrings.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    for (piece in pieces) {
                        val cookie = Cookie.parse(url!!, piece) ?: continue
                        if (cookies == null) {
                            cookies = ArrayList()
                        }
                        cookies.add(cookie)
                    }
                    return cookies ?: emptyList()
                }

                fun addCookie(domain: String, cookie: Cookie) {
                    EhApplication.ehCookieStore.addCookie(
                        EhCookieStore.newCookie(
                            cookie,
                            domain,
                            forcePersistent = true,
                            forceLongLive = true,
                            forceNotHostOnly = true
                        )
                    )
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (present) {
                        view.destroy()
                        return
                    }
                    val httpUrl = url.toHttpUrlOrNull() ?: return
                    val cookieString = CookieManager.getInstance().getCookie(EhUrl.HOST_E)
                    val cookies = parseCookies(httpUrl, cookieString)
                    var getId = false
                    var getHash = false
                    for (cookie in cookies) {
                        if (EhCookieStore.KEY_IPB_MEMBER_ID == cookie.name) {
                            getId = true
                        } else if (EhCookieStore.KEY_IPB_PASS_HASH == cookie.name) {
                            getHash = true
                        }
                        addCookie(EhUrl.DOMAIN_EX, cookie)
                        addCookie(EhUrl.DOMAIN_E, cookie)
                    }
                    if (getId && getHash) {
                        present = true
                        coroutineScope.launchNonCancellable {
                            postLogin()
                        }
                        navController.navigate(SELECT_SITE_ROUTE_NAME)
                    }
                }
            }
            webChromeClient = DialogWebChromeClient(context)
            loadUrl(EhUrl.URL_SIGN_IN)
        }
    })
}
