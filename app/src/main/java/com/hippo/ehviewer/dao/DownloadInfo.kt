package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.hippo.ehviewer.client.data.AbstractGalleryInfo
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Entity(tableName = "DOWNLOADS")
class DownloadInfo(
    @Ignore
    val galleryInfo: GalleryInfo = BaseGalleryInfo(),
) : BaseGalleryInfo(), AbstractGalleryInfo by galleryInfo {
    constructor() : this(galleryInfo = BaseGalleryInfo())

    @ColumnInfo(name = "STATE")
    var state = 0

    @ColumnInfo(name = "LEGACY")
    var legacy = 0

    @ColumnInfo(name = "TIME")
    var time: Long = 0

    @ColumnInfo(name = "LABEL")
    var label: String? = null

    @ColumnInfo(name = "POSITION")
    var position: Int = 0

    @Ignore
    var speed: Long = 0

    @Ignore
    var remaining: Long = 0

    @Ignore
    var finished = 0

    @Ignore
    var downloaded = 0

    @Ignore
    var total = 0

    companion object {
        const val STATE_INVALID = -1
        const val STATE_NONE = 0
        const val STATE_WAIT = 1
        const val STATE_DOWNLOAD = 2
        const val STATE_FINISH = 3
        const val STATE_FAILED = 4
    }
}
