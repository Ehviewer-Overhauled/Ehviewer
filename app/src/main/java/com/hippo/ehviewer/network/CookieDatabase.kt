/*
 * Copyright 2017 Hippo Seven
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
package com.hippo.ehviewer.network

import android.content.Context
import androidx.room.Room
import com.hippo.ehviewer.dao.Cookie
import com.hippo.ehviewer.dao.CookiesDatabase
import okhttp3.Cookie as OkHttpCookie

class CookieDatabase(context: Context) {
    private val cookiesList by lazy {
        val now = System.currentTimeMillis()
        db.cookiesDao().list().mapNotNull {
            it.takeUnless { !it.persistent || it.expiresAt <= now } ?: run {
                db.cookiesDao().delete(it)
                null
            }
        }.toMutableList()
    }
    private val db = Room.databaseBuilder(context, CookiesDatabase::class.java, "okhttp3-cookie.db").build()

    val allCookies by lazy {
        hashMapOf<String, CookieSet>().also { map ->
            cookiesList.map { it.toOkHttp3Cookie() }.forEach {
                val set = map[it.domain] ?: CookieSet().apply { map[it.domain] = this }
                set.add(it)
            }
        }
    }

    private fun findCookieWithOkHttpCookies(cookie: OkHttpCookie): Cookie? {
        return cookiesList.find { it.name == cookie.name && it.domain == cookie.domain && it.value == cookie.value }
    }

    fun add(cookie: OkHttpCookie) {
        val c = cookie.toCookie()
        c.id = db.cookiesDao().insert(c)
        cookiesList.add(c)
    }

    fun update(from: OkHttpCookie, to: OkHttpCookie) {
        val origin = findCookieWithOkHttpCookies(from) ?: return
        val new = to.toCookie(origin.id)
        cookiesList.remove(origin)
        cookiesList.add(new)
        db.cookiesDao().update(new)
    }

    fun remove(cookie: OkHttpCookie) {
        val origin = findCookieWithOkHttpCookies(cookie) ?: return
        db.cookiesDao().delete(origin)
        cookiesList.remove(origin)
    }

    fun clear() {
        db.clearAllTables()
        cookiesList.clear()
    }
}

fun Cookie.toOkHttp3Cookie(): OkHttpCookie {
    return OkHttpCookie.Builder().apply {
        name(name)
        value(value)
        expiresAt(expiresAt)
        if (hostOnly) hostOnlyDomain(domain) else domain(domain)
        path(path)
        if (secure) secure()
        if (httpOnly) httpOnly()
    }.build()
}

private fun OkHttpCookie.toCookie(id: Long? = null): Cookie {
    return Cookie(
        name, value, expiresAt, domain, path, secure, httpOnly, persistent, hostOnly, id,
    )
}
