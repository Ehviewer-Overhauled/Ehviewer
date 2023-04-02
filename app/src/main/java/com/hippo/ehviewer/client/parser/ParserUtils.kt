/*
 * Copyright 2015 Hippo Seven
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

import com.hippo.yorozuya.NumberUtils
import com.hippo.yorozuya.StringUtils
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object ParserUtils {
    private val formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US).withZone(ZoneOffset.UTC)

    @Synchronized
    fun formatDate(time: Long): String {
        return formatter.format(Instant.ofEpochMilli(time))
    }

    fun trim(str: String?): String {
        return str?.let { StringUtils.unescapeXml(it).trim() } ?: ""
    }

    fun parseInt(str: String?, defValue: Int): Int {
        return NumberUtils.parseIntSafely(trim(str).replace(",", ""), defValue)
    }

    fun parseLong(str: String?, defValue: Long): Long {
        return NumberUtils.parseLongSafely(trim(str).replace(",", ""), defValue)
    }
}
