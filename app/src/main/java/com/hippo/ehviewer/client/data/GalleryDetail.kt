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

import kotlinx.parcelize.Parcelize

@Parcelize // This is wrong Parcelize usage, TODO: use delegate and move galleryinfo to primary constructor
class GalleryDetail(
    @JvmField
    var apiUid: Long = -1L,

    @JvmField
    var apiKey: String? = null,

    @JvmField
    var torrentCount: Int = 0,

    @JvmField
    var torrentUrl: String? = null,

    @JvmField
    var archiveUrl: String? = null,

    @JvmField
    var parent: String? = null,

    @JvmField
    var newerVersions: ArrayList<GalleryInfo> = ArrayList(),

    @JvmField
    var visible: String? = null,

    @JvmField
    var language: String? = null,

    @JvmField
    var size: String? = null,

    @JvmField
    var favoriteCount: Int = 0,

    @JvmField
    var isFavorited: Boolean = false,

    @JvmField
    var ratingCount: Int = 0,

    @JvmField
    var tags: Array<GalleryTagGroup>? = null,

    @JvmField
    var comments: GalleryCommentList? = null,

    @JvmField
    var previewPages: Int = 0,

    @JvmField
    var previewSet: PreviewSet? = null,
) : GalleryInfo()