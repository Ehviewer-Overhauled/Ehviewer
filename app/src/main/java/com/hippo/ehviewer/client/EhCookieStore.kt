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
import java.util.regex.Pattern

object EhCookieStore : CookieJar {
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
    const val KEY_SETTINGS_PROFILE = "sp"
    private const val KEY_STAR = "star"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie: Cookie = Cookie.Builder().name(KEY_CONTENT_WARNING).value(CONTENT_WARNING_NOT_SHOW).domain(EhUrl.DOMAIN_E).path("/").expiresAt(Long.MAX_VALUE).build()

    fun newCookie(
        cookie: Cookie,
        newDomain: String,
        forcePersistent: Boolean = false,
        forceLongLive: Boolean = false,
        forceNotHostOnly: Boolean = false,
    ): Cookie {
        val builder = Cookie.Builder()
        builder.name(cookie.name)
        builder.value(cookie.value)
        if (forceLongLive) {
            builder.expiresAt(Long.MAX_VALUE)
        } else if (cookie.persistent) {
            builder.expiresAt(cookie.expiresAt)
        } else if (forcePersistent) {
            builder.expiresAt(Long.MAX_VALUE)
        }
        if (cookie.hostOnly && !forceNotHostOnly) {
            builder.hostOnlyDomain(newDomain)
        } else {
            builder.domain(newDomain)
        }
        builder.path(cookie.path)
        if (cookie.secure) {
            builder.secure()
        }
        if (cookie.httpOnly) {
            builder.httpOnly()
        }
        return builder.build()
    }

    fun copyNecessaryCookies() {
        val cookie = get(EhUrl.HOST_E.toHttpUrl()).filter { it.name == KEY_STAR || it.name == KEY_IPB_MEMBER_ID || it.name == KEY_IPB_PASS_HASH || it.name == KEY_IGNEOUS }
        cookie.forEach { manager.setCookie(EhUrl.HOST_EX, it.toString()) }
    }

    fun deleteCookie(url: HttpUrl, name: String) {
        val deletedCookie = Cookie.Builder().name(name).value("deleted").domain(url.host).expiresAt(0).build()
        manager.setCookie(url.toString(), deletedCookie.toString())
    }

    fun addCookie(cookie: Cookie) {
        if (EhUrl.DOMAIN_E in cookie.domain) manager.setCookie(EhUrl.HOST_E, cookie.toString()) else manager.setCookie(EhUrl.DOMAIN_EX, cookie.toString())
    }

    fun getCookieHeader(url: HttpUrl): String {
        val cookies = get(url)
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
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = cookies.filterNot { it.name == KEY_UTMP_NAME }.forEach { manager.setCookie(url.toString(), it.toString()) }

    /**
     * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
     * of Android's private InetAddress#isNumeric API.
     *
     *
     * This matches IPv6 addresses as a hex string containing at least one colon, and possibly
     * including dots after the first colon. It matches IPv4 addresses as strings containing only
     * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
     * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
     * verification).
     */
    private val VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)")

    /**
     * Returns true if `host` is not a host name and might be an IP address.
     */
    private fun verifyAsIpAddress(host: String): Boolean {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches()
    }

    // okhttp3.Cookie.domainMatch(HttpUrl, String)
    @JvmStatic
    fun domainMatch(url: HttpUrl, domain: String?): Boolean {
        val urlHost = url.host
        return if (urlHost == domain) {
            true // As in 'example.com' matching 'example.com'.
        } else {
            urlHost.endsWith(domain!!) && urlHost[urlHost.length - domain.length - 1] == '.' && !verifyAsIpAddress(urlHost)
        }
        // As in 'example.com' matching 'www.example.com'.
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val checkTips = domainMatch(url, EhUrl.DOMAIN_E)
        return get(url).run {
            if (checkTips) {
                filterNot { it.name == KEY_CONTENT_WARNING }.toMutableList().apply { add(sTipsCookie) }
            } else {
                this
            }
        }
    }
}
