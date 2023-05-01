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

import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview

// ExHentai Large Preview: https://exhentai.org/t/***
// ExHentai Normal Preview: https://exhentai.org/m/***
// EHentai Normal Preview: https://ehgt.org/m/***
// EHentai Large Preview: https://ehgt.org/***

private const val URL_PREFIX_THUMB_E = "https://ehgt.org/"
private const val URL_PREFIX_THUMB_EX = "https://exhentai.org/"

fun getImageKey(gid: Long, index: Int): String {
    return "image:$gid:$index"
}

fun getPreviewThumbKey(url: String): String {
    return url.removePrefix(URL_PREFIX_THUMB_E)
        .removePrefix(URL_PREFIX_THUMB_EX)
        .removePrefix("t/")
}

val GalleryPreview.url
    get() = if (this is NormalGalleryPreview) {
        if (EhUtils.isExHentai) URL_PREFIX_THUMB_EX + imageKey else URL_PREFIX_THUMB_E + imageKey
    } else {
        if (EhUtils.isExHentai) URL_PREFIX_THUMB_EX + "t/" + imageKey else URL_PREFIX_THUMB_E + imageKey
    }
