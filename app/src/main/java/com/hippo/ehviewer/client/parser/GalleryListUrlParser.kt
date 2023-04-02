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
package com.hippo.ehviewer.client.parser

import android.net.Uri
import android.os.Build
import android.text.TextUtils
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.yorozuya.Utilities
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object GalleryListUrlParser {
    private val VALID_HOSTS = arrayOf(EhUrl.DOMAIN_EX, EhUrl.DOMAIN_E, EhUrl.DOMAIN_LOFI)
    private const val PATH_NORMAL = "/"
    private const val PATH_UPLOADER = "/uploader/"
    private const val PATH_TAG = "/tag/"
    private const val PATH_TOPLIST = "/toplist.php"
    fun parse(urlStr: String): ListUrlBuilder? {
        val url = try {
            URL(urlStr)
        } catch (e: MalformedURLException) {
            return null
        }
        if (!Utilities.contain(VALID_HOSTS, url.host)) {
            return null
        }
        val path = url.path ?: return null
        return if (PATH_NORMAL == path || path.isEmpty()) {
            val builder = ListUrlBuilder()
            builder.setQuery(url.query)
            builder
        } else if (path.startsWith(PATH_UPLOADER)) {
            parseUploader(path)
        } else if (path.startsWith(PATH_TAG)) {
            parseTag(path)
        } else if (path.startsWith(PATH_TOPLIST)) {
            parseToplist(urlStr)
        } else if (path.startsWith("/")) {
            val category = try {
                path.substring(1).toInt()
            } catch (e: NumberFormatException) {
                return null
            }
            val builder = ListUrlBuilder()
            builder.setQuery(url.query)
            builder.category = category
            builder
        } else {
            null
        }
    }

    // TODO get page
    private fun parseUploader(path: String): ListUrlBuilder? {
        var uploader: String?
        val prefixLength = PATH_UPLOADER.length
        val index = path.indexOf('/', prefixLength)
        uploader = if (index < 0) {
            path.substring(prefixLength)
        } else {
            path.substring(prefixLength, index)
        }
        uploader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            URLDecoder.decode(uploader, StandardCharsets.UTF_8)
        } else {
            try {
                URLDecoder.decode(uploader, StandardCharsets.UTF_8.displayName())
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return null
            }
        }
        if (TextUtils.isEmpty(uploader)) {
            return null
        }
        val builder = ListUrlBuilder()
        builder.mode = ListUrlBuilder.MODE_UPLOADER
        builder.keyword = uploader
        return builder
    }

    // TODO get page
    private fun parseTag(path: String): ListUrlBuilder? {
        var tag: String?
        val prefixLength = PATH_TAG.length
        val index = path.indexOf('/', prefixLength)
        tag = if (index < 0) {
            path.substring(prefixLength)
        } else {
            path.substring(prefixLength, index)
        }
        tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            URLDecoder.decode(tag, StandardCharsets.UTF_8)
        } else {
            try {
                URLDecoder.decode(tag, StandardCharsets.UTF_8.displayName())
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return null
            }
        }
        if (TextUtils.isEmpty(tag)) {
            return null
        }
        val builder = ListUrlBuilder()
        builder.mode = ListUrlBuilder.MODE_TAG
        builder.keyword = tag
        return builder
    }

    // TODO get page
    private fun parseToplist(path: String): ListUrlBuilder? {
        val uri = Uri.parse(path)
        if (uri == null || TextUtils.isEmpty(uri.getQueryParameter("tl"))) {
            return null
        }
        val tl = uri.getQueryParameter("tl")!!.toInt()
        if (tl > 15 || tl < 11) {
            return null
        }
        val builder = ListUrlBuilder()
        builder.mode = ListUrlBuilder.MODE_TOPLIST
        builder.keyword = tl.toString()
        return builder
    }
}
