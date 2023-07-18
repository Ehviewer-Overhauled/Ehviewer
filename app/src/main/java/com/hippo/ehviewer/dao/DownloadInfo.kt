package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo

@Entity(tableName = "DOWNLOADS")
class DownloadInfo() : BaseGalleryInfo() {
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

    constructor(galleryInfo: GalleryInfo) : this() {
        gid = galleryInfo.gid
        token = galleryInfo.token
        title = galleryInfo.title
        titleJpn = galleryInfo.titleJpn
        thumbKey = galleryInfo.thumbKey
        this.category = galleryInfo.category
        posted = galleryInfo.posted
        uploader = galleryInfo.uploader
        rating = galleryInfo.rating
        simpleTags = galleryInfo.simpleTags
        simpleLanguage = galleryInfo.simpleLanguage
    }

    companion object {
        const val STATE_INVALID = -1
        const val STATE_NONE = 0
        const val STATE_WAIT = 1
        const val STATE_DOWNLOAD = 2
        const val STATE_FINISH = 3
        const val STATE_FAILED = 4
    }
}
