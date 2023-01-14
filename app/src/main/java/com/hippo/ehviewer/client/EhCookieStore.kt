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
import com.hippo.network.CookieRepository
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Collections

object EhCookieStore : CookieRepository(EhApplication.application, "okhttp3-cookie.db") {
    fun signOut() {
        clear()
    }

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E.toHttpUrl()
        return contains(url, KEY_IPD_MEMBER_ID) &&
                contains(url, KEY_IPD_PASS_HASH)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = super.loadForRequest(url)
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
}