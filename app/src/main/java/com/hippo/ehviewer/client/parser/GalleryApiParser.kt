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

import com.hippo.ehviewer.client.EhUtils.getCategory
import com.hippo.ehviewer.client.EhUtils.handleThumbUrlResolution
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.yorozuya.NumberUtils
import org.json.JSONObject

object GalleryApiParser {
    fun parse(body: String, galleryInfoList: List<GalleryInfo>) {
        val jo = JSONObject(body)
        val ja = jo.getJSONArray("gmetadata")
        for (i in 0 until ja.length()) {
            val g = ja.getJSONObject(i)
            val gid = g.getLong("gid")
            val gi = galleryInfoList.find { it.gid == gid } ?: continue
            gi.title = ParserUtils.trim(g.getString("title"))
            gi.titleJpn = ParserUtils.trim(g.getString("title_jpn"))
            gi.category = getCategory(g.getString("category"))
            gi.thumb = handleThumbUrlResolution(g.getString("thumb"))
            gi.uploader = g.getString("uploader")
            gi.posted =
                ParserUtils.formatDate(ParserUtils.parseLong(g.getString("posted"), 0) * 1000)
            gi.rating = NumberUtils.parseFloatSafely(g.getString("rating"), 0.0f)
            // tags
            val tagJa = g.getJSONArray("tags")
            gi.simpleTags = (0 until tagJa.length()).map { tagJa.getString(it) }.toTypedArray()
            gi.pages = NumberUtils.parseIntSafely(g.getString("filecount"), 0)
            gi.generateSLang()
        }
    }
}
