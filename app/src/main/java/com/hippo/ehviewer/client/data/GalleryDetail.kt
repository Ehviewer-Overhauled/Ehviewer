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
package com.hippo.ehviewer.client.data

import android.os.Parcelable
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.S_LANGS
import kotlinx.parcelize.Parcelize

private val LANGUAGES = arrayOf(
    "English",
    "Chinese",
    "Spanish",
    "Korean",
    "Russian",
    "French",
    "Portuguese",
    "Thai",
    "German",
    "Italian",
    "Vietnamese",
    "Polish",
    "Hungarian",
    "Dutch",
)

@Parcelize
class GalleryDetail(
    val galleryInfo: BaseGalleryInfo = BaseGalleryInfo(),
    var apiUid: Long = -1L,
    var apiKey: String? = null,
    var torrentCount: Int = 0,
    var torrentUrl: String? = null,
    var archiveUrl: String? = null,
    var parent: String? = null,
    var newerVersions: ArrayList<BaseGalleryInfo> = ArrayList(),
    var visible: String? = null,
    var language: String? = null,
    var size: String? = null,
    var favoriteCount: Int = 0,
    var ratingCount: Int = 0,
    val tags: Array<GalleryTagGroup>,
    var comments: GalleryCommentList,
    val previewPages: Int,
    val previewList: List<GalleryPreview>,
) : GalleryInfo by galleryInfo, Parcelable {
    override fun generateSLang() {
        val index = LANGUAGES.indexOf(language)
        if (index != -1) simpleLanguage = S_LANGS[index]
    }
}
