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

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

object EhCookieStore : CookieJar, Interceptor {
    private val manager = CookieManager.getInstance()
    fun signOut() = manager.removeAllCookies(null)
    fun contains(url: HttpUrl, name: String) = get(url).any { it.name == name }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())

        return if (cookies != null && cookies.isNotEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }.filterNot { it.name == KEY_UTMP_NAME }
        } else {
            emptyList()
        }
    }

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E.toHttpUrl()
        return contains(url, KEY_IPB_MEMBER_ID) && contains(url, KEY_IPB_PASS_HASH)
    }

    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private const val KEY_STAR = "star"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = Cookie.Builder().apply {
        name(KEY_CONTENT_WARNING)
        value(CONTENT_WARNING_NOT_SHOW)
        domain(EhUrl.DOMAIN_E)
        path("/")
        expiresAt(Long.MAX_VALUE)
    }.build()

    fun copyNecessaryCookies() {
        val cookie = get(EhUrl.HOST_E.toHttpUrl()).filter { it.name == KEY_STAR || it.name == KEY_IPB_MEMBER_ID || it.name == KEY_IPB_PASS_HASH || it.name == KEY_IGNEOUS }
        cookie.forEach { manager.setCookie(EhUrl.HOST_EX, it.toString()) }
        flush()
    }

    fun deleteCookie(url: HttpUrl, name: String) {
        manager.setCookie(url.toString(), "$name=;Max-Age=0")
    }

    fun addCookie(cookie: Cookie) {
        if (EhUrl.DOMAIN_E in cookie.domain) manager.setCookie(EhUrl.HOST_E, cookie.toString()) else manager.setCookie(EhUrl.DOMAIN_EX, cookie.toString())
    }

    fun flush() = manager.flush()

    fun getCookieHeader(url: HttpUrl): String {
        val cookies = loadForRequest(url)
        val cookieHeader = StringBuilder()
        var i = 0
        val size = cookies.size
        while (i < size) {
            if (i > 0) {
                cookieHeader.append("; ")
            }
            val cookie = cookies[i]
            cookieHeader.append(cookie.name).append('=').append(cookie.value)
            i++
        }
        return cookieHeader.toString()
    }

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = cookies.filterNot { it.name == KEY_UTMP_NAME }.forEach { manager.setCookie(url.toString(), it.toString()) }.also { flush() }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val checkTips = EhUrl.DOMAIN_E in url.host
        return get(url).run {
            if (checkTips) {
                filterNot { it.name == KEY_CONTENT_WARNING }.toMutableList().apply { add(sTipsCookie) }
            } else {
                this
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val newRequest = request.newBuilder().addHeader("Cookie", getCookieHeader(url)).build()
        val response = chain.proceed(newRequest)
        saveFromResponse(url, Cookie.parseAll(url, response.headers))
        return response
    }
}
