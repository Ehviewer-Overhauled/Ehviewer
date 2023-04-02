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

import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.yorozuya.StringUtils
import java.util.regex.Pattern

object GalleryPageParser {
    private val PATTERN_IMAGE_URL = Pattern.compile("<img[^>]*src=\"([^\"]+)\" style")
    private val PATTERN_SKIP_HATH_KEY = Pattern.compile("onclick=\"return nl\\('([^)]+)'\\)")
    private val PATTERN_ORIGIN_IMAGE_URL =
        Pattern.compile("<a href=\"([^\"]+)fullimg.php([^\"]+)\">")

    // TODO Not sure about the size of show keys
    private val PATTERN_SHOW_KEY = Pattern.compile("var showkey=\"([0-9a-z]+)\";")

    fun parse(body: String): Result {
        var m = PATTERN_IMAGE_URL.matcher(body)
        val imageUrl = if (m.find()) {
            StringUtils.unescapeXml(StringUtils.trim(m.group(1)))
        } else {
            null
        }
        m = PATTERN_SKIP_HATH_KEY.matcher(body)
        val skipHathKey = if (m.find()) {
            StringUtils.unescapeXml(StringUtils.trim(m.group(1)))
        } else {
            null
        }
        m = PATTERN_ORIGIN_IMAGE_URL.matcher(body)
        val originImageUrl = if (m.find()) {
            StringUtils.unescapeXml(m.group(1)) + "fullimg.php" +
                StringUtils.unescapeXml(m.group(2))
        } else {
            null
        }
        m = PATTERN_SHOW_KEY.matcher(body)
        val showKey = if (m.find()) {
            m.group(1)
        } else {
            null
        }
        return if (!imageUrl.isNullOrEmpty() && !showKey.isNullOrEmpty()) {
            Result(imageUrl, skipHathKey, originImageUrl, showKey)
        } else {
            throw ParseException("Parse image url and show error", body)
        }
    }

    class Result(
        val imageUrl: String,
        val skipHathKey: String?,
        val originImageUrl: String?,
        val showKey: String,
    )
}
