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
import com.hippo.ehviewer.client.parseAs
import com.hippo.ehviewer.util.unescapeXml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object GalleryPageApiParser {
    private val PATTERN_IMAGE_URL = Regex("<img[^>]*src=\"([^\"]+)\" style")
    private val PATTERN_SKIP_HATH_KEY = Regex("onclick=\"return nl\\('([^)]+)'\\)")
    private val PATTERN_ORIGIN_IMAGE_URL = Regex("<a href=\"([^\"]+)fullimg.php([^\"]+)\">")

    fun parse(body: String): Result {
        runCatching { body.parseAs<Error>() }.onSuccess { throw ParseException(it.error, body) }
        val res = body.parseAs<JsonResult>()
        val imageUrl = PATTERN_IMAGE_URL.find(res.imageUrl)?.run {
            groupValues[1].trim().unescapeXml()
        }
        val skipHathKey = PATTERN_SKIP_HATH_KEY.find(res.skipHathKey)?.run {
            groupValues[1].trim().unescapeXml()
        }
        val originImageUrl = PATTERN_ORIGIN_IMAGE_URL.find(res.originImageUrl)?.run {
            groupValues[1].unescapeXml() + "fullimg.php" + groupValues[2].unescapeXml()
        }
        if (!imageUrl.isNullOrEmpty()) {
            return Result(imageUrl, skipHathKey, originImageUrl)
        } else {
            throw ParseException("Parse image url and skip hath key error", body)
        }
    }

    @Serializable
    private data class JsonResult(
        @SerialName("i3")
        val imageUrl: String,
        @SerialName("i6")
        val skipHathKey: String,
        @SerialName("i7")
        val originImageUrl: String,
    )

    class Result(val imageUrl: String, val skipHathKey: String?, val originImageUrl: String?)
}
