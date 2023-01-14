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

import com.hippo.ehviewer.EhApplication
import com.hippo.network.CookieDatabase
import com.hippo.network.CookieSet
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Collections
import java.util.regex.Pattern

object EhCookieStore : CookieJar {
    private val db: CookieDatabase = CookieDatabase(EhApplication.application, "okhttp3-cookie.db")
    private val map: MutableMap<String, CookieSet> = db.allCookies

    fun signOut() {
        clear()
    }

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E.toHttpUrl()
        return contains(url, KEY_IPD_MEMBER_ID) &&
                contains(url, KEY_IPD_PASS_HASH)
    }

    const val KEY_IPD_MEMBER_ID = "ipb_member_id"
    const val KEY_IPD_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private val sTipsCookie: Cookie = Cookie.Builder()
        .name(EhConfig.KEY_CONTENT_WARNING)
        .value(EhConfig.CONTENT_WARNING_NOT_SHOW)
        .domain(EhUrl.DOMAIN_E)
        .path("/")
        .expiresAt(Long.MAX_VALUE)
        .build()

    fun newCookie(
        cookie: Cookie, newDomain: String, forcePersistent: Boolean,
        forceLongLive: Boolean, forceNotHostOnly: Boolean
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

    @Synchronized
    fun addCookie(cookie: Cookie) {
        // For cookie database
        var toAdd: Cookie? = null
        var toUpdate: Cookie? = null
        var toRemove: Cookie? = null
        var set = map[cookie.domain]
        if (set == null) {
            set = CookieSet()
            map[cookie.domain] = set
        }
        if (cookie.expiresAt <= System.currentTimeMillis()) {
            toRemove = set.remove(cookie)
            // If the cookie is not persistent, it's not in database
            if (toRemove != null && !toRemove.persistent) {
                toRemove = null
            }
        } else {
            toAdd = cookie
            toUpdate = set.add(cookie)
            // If the cookie is not persistent, it's not in database
            if (!toAdd.persistent) toAdd = null
            if (toUpdate != null && !toUpdate.persistent) toUpdate = null
            // Remove the cookie if it updates to null
            if (toAdd == null && toUpdate != null) {
                toRemove = toUpdate
                toUpdate = null
            }
        }
        if (toRemove != null) {
            db.remove(toRemove)
        }
        if (toAdd != null) {
            if (toUpdate != null) {
                db.update(toUpdate, toAdd)
            } else {
                db.add(toAdd)
            }
        }
    }

    fun getCookieHeader(url: HttpUrl): String {
        val cookies = getCookies(url)
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

    @Synchronized
    fun getCookies(url: HttpUrl): List<Cookie> {
        val accepted: MutableList<Cookie> = ArrayList()
        val expired: MutableList<Cookie> = ArrayList()
        for ((domain, cookieSet) in map) {
            if (domainMatch(url, domain)) {
                cookieSet[url, accepted, expired]
            }
        }
        for (cookie in expired) {
            if (cookie.persistent) {
                db.remove(cookie)
            }
        }

        // RFC 6265 Section-5.4 step 2, sort the cookie-list
        // Cookies with longer paths are listed before cookies with shorter paths.
        // Ignore creation-time, we don't store them.
        accepted.sortWith { o1: Cookie, o2: Cookie -> o2.path.length - o1.path.length }
        return accepted
    }

    fun contains(url: HttpUrl, name: String?): Boolean {
        for (cookie in getCookies(url)) {
            if (cookie.name == name) {
                return true
            }
        }
        return false
    }

    /**
     * Remove all cookies in this `CookieRepository`.
     */
    @Synchronized
    fun clear() {
        map.clear()
        db.clear()
    }

    @Synchronized
    fun close() {
        db.close()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            addCookie(cookie)
        }
    }

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
    private val VERIFY_AS_IP_ADDRESS =
        Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)")

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
        } else urlHost.endsWith(domain!!)
                && urlHost[urlHost.length - domain.length - 1] == '.' && !verifyAsIpAddress(
            urlHost
        )
        // As in 'example.com' matching 'www.example.com'.
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = getCookies(url)
        val checkTips = domainMatch(url, EhUrl.DOMAIN_E)
        return if (checkTips) {
            val result: MutableList<Cookie> = ArrayList(cookies.size + 1)
            // Add all but skip some
            for (cookie in cookies) {
                val name = cookie.name
                if (EhConfig.KEY_CONTENT_WARNING == name) {
                    continue
                }
                if (EhConfig.KEY_UCONFIG == name) {
                    continue
                }
                result.add(cookie)
            }
            // Add some
            result.add(sTipsCookie)
            Collections.unmodifiableList(result)
        } else {
            cookies
        }
    }
}