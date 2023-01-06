package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Entity(tableName = "BOOKMARKS")
class BookmarkInfo : BaseGalleryInfo() {
    @JvmField
    @ColumnInfo(name = "PAGE")
    var page = 0

    @JvmField
    @ColumnInfo(name = "TIME")
    var time: Long = 0
}