/*
 * Copyright 2019 Hippo Seven
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

import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.yorozuya.StringUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Matcher
import java.util.regex.Pattern

object GalleryPageApiParser {
    private val PATTERN_IMAGE_URL = Pattern.compile("<img[^>]*src=\"([^\"]+)\" style")
    private val PATTERN_SKIP_HATH_KEY = Pattern.compile("onclick=\"return nl\\('([^)]+)'\\)")
    private val PATTERN_ORIGIN_IMAGE_URL =
        Pattern.compile("<a href=\"([^\"]+)fullimg.php([^\"]+)\">")

    fun parse(body: String): Result {
        return try {
            var m: Matcher
            val jo = JSONObject(body)
            if (jo.has("error")) {
                throw ParseException(jo.getString("error"), body)
            }
            val i3 = jo.getString("i3")
            m = PATTERN_IMAGE_URL.matcher(i3)
            val imageUrl = if (m.find()) {
                StringUtils.unescapeXml(StringUtils.trim(m.group(1)))
            } else {
                null
            }
            val i6 = jo.getString("i6")
            m = PATTERN_SKIP_HATH_KEY.matcher(i6)
            val skipHathKey = if (m.find()) {
                StringUtils.unescapeXml(StringUtils.trim(m.group(1)))
            } else {
                null
            }
            val i7 = jo.getString("i7")
            m = PATTERN_ORIGIN_IMAGE_URL.matcher(i7)
            val originImageUrl = if (m.find()) {
                StringUtils.unescapeXml(m.group(1)) + "fullimg.php" +
                    StringUtils.unescapeXml(m.group(2))
            } else {
                null
            }
            if (!imageUrl.isNullOrEmpty()) {
                Result(imageUrl, skipHathKey, originImageUrl)
            } else {
                throw ParseException("Parse image url and skip hath key error", body)
            }
        } catch (e: JSONException) {
            throw ParseException("Can't parse json", body, e)
        }
    }

    class Result(val imageUrl: String, val skipHathKey: String?, val originImageUrl: String?)
}
