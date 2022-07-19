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
package com.hippo.network

import com.hippo.util.HashCodeUtils
import com.hippo.yorozuya.ObjectUtils
import okhttp3.Cookie
import okhttp3.HttpUrl

internal class CookieSet {
    private val map: MutableMap<Key, Cookie> = HashMap()

    /**
     * Adds a cookie to this `CookieSet`.
     * Returns a previous cookie with
     * the same name, domain and path or `null`.
     */
    fun add(cookie: Cookie): Cookie? {
        return map.put(Key(cookie), cookie)
    }

    /**
     * Removes a cookie with the same name,
     * domain and path as the cookie.
     * Returns the removed cookie or `null`.
     */
    fun remove(cookie: Cookie): Cookie? {
        return map.remove(Key(cookie))
    }

    /**
     * Get cookies for the url. Fill `accepted` and `expired`.
     */
    operator fun get(url: HttpUrl?, accepted: MutableList<Cookie>, expired: MutableList<Cookie>) {
        val now = System.currentTimeMillis()
        val iterator: MutableIterator<Map.Entry<Key, Cookie>> = map.entries.iterator()
        while (iterator.hasNext()) {
            val cookie = iterator.next().value
            if (cookie.expiresAt <= now) {
                iterator.remove()
                expired.add(cookie)
            } else if (cookie.matches(url!!)) {
                accepted.add(cookie)
            }
        }
    }

    internal class Key(cookie: Cookie) {
        private val name: String
        private val domain: String
        private val path: String
        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other is Key) {
                return ObjectUtils.equal(other.name, name) &&
                        ObjectUtils.equal(other.domain, domain) &&
                        ObjectUtils.equal(other.path, path)
            }
            return false
        }

        override fun hashCode(): Int {
            return HashCodeUtils.hashCode(name, domain, path)
        }

        init {
            name = cookie.name
            domain = cookie.domain
            path = cookie.path
        }
    }
}