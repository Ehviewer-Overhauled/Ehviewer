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
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
open class GalleryInfoImpl @JvmOverloads constructor(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    override var gid: Long = 0,

    @ColumnInfo(name = "TOKEN")
    override var token: String? = null,

    @ColumnInfo(name = "TITLE")
    override var title: String? = null,

    @ColumnInfo(name = "TITLE_JPN")
    override var titleJpn: String? = null,

    @ColumnInfo(name = "THUMB")
    override var thumb: String? = null,

    @ColumnInfo(name = "CATEGORY")
    override var category: Int = 0,

    @ColumnInfo(name = "POSTED")
    override var posted: String? = null,

    @ColumnInfo(name = "UPLOADER")
    override var uploader: String? = null,

    @Ignore
    override var disowned: Boolean = false,

    @ColumnInfo(name = "RATING")
    override var rating: Float = 0f,

    @Ignore
    override var rated: Boolean = false,

    @Ignore
    override var simpleTags: Array<String>? = null,

    @Ignore
    override var pages: Int = 0,

    @Ignore
    override var thumbWidth: Int = 0,

    @Ignore
    override var thumbHeight: Int = 0,

    @Ignore
    override var spanSize: Int = 0,

    @Ignore
    override var spanIndex: Int = 0,

    @Ignore
    override var spanGroupIndex: Int = 0,

    @ColumnInfo(name = "SIMPLE_LANGUAGE")
    override var simpleLanguage: String? = null,

    @Ignore
    override var favoriteSlot: Int = -2,

    @Ignore
    override var favoriteName: String? = null

) : GalleryInfo, Parcelable