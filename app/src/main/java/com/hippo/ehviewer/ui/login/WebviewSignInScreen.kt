package com.hippo.ehviewer.ui.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.FINISH_ROUTE_NAME
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.SELECT_SITE_ROUTE_NAME
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebviewSignInScreen() {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    val client = remember {
        object : AccompanistWebViewClient() {
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
                EhCookieStore.addCookie(
                    EhCookieStore.newCookie(
                        cookie,
                        domain,
                        forcePersistent = true,
                        forceLongLive = true,
                        forceNotHostOnly = true,
                    ),
                )
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (present) {
                    view.destroy()
                    return
                }
                val httpUrl = url?.toHttpUrl() ?: return
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
                }
                if (getId && getHash) {
                    present = true
                    coroutineScope.launchIO {
                        val canEx = withNonCancellableContext {
                            cookies.forEach {
                                addCookie(EhUrl.DOMAIN_EX, it)
                                addCookie(EhUrl.DOMAIN_E, it)
                            }
                            postLogin()
                        }
                        withUIContext { navController.navigate(if (canEx) SELECT_SITE_ROUTE_NAME else FINISH_ROUTE_NAME) }
                    }
                }
            }
        }
    }
    SideEffect {
        EhUtils.signOut()
        CookieManager.getInstance().apply {
            flush()
            removeAllCookies(null)
            removeSessionCookies(null)
        }
    }
    WebView(
        state = state,
        onCreated = {
            it.settings.run {
                builtInZoomControls = true
                displayZoomControls = true
                javaScriptEnabled = true
            }
        },
        client = client,
    )
}
