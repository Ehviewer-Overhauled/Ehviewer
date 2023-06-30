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

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview

// ExHentai Large Preview: https://s.exhentai.org/t/***
// ExHentai Normal Preview: https://s.exhentai.org/m/***
// EHentai Normal Preview: https://ehgt.org/m/***
// EHentai Large Preview: https://ehgt.org/***

private const val URL_PREFIX_THUMB_E = "https://ehgt.org/"
private const val URL_PREFIX_THUMB_EX = "https://s.exhentai.org/"
private const val URL_PREFIX_LARGE_THUMB_EX = URL_PREFIX_THUMB_EX + "t/"

fun getImageKey(gid: Long, index: Int): String {
    return "image:$gid:$index"
}

fun getThumbKey(url: String): String {
    return url.removePrefix(URL_PREFIX_THUMB_E).removePrefix(URL_PREFIX_LARGE_THUMB_EX).removePrefix(URL_PREFIX_THUMB_EX)
}

val GalleryPreview.url
    get() = if (exThumb) {
        if (this is NormalGalleryPreview) URL_PREFIX_THUMB_EX else URL_PREFIX_LARGE_THUMB_EX
    } else {
        URL_PREFIX_THUMB_E
    } + imageKey

val GalleryInfo.thumbUrl
    get() = (if (exThumb) URL_PREFIX_LARGE_THUMB_EX else URL_PREFIX_THUMB_E) + thumbKey!!

val exThumb get() = EhUtils.isExHentai && !Settings.forceEhThumb
