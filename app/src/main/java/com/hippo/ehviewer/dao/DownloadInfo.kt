package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfoImpl

@Entity(tableName = "DOWNLOADS")
class DownloadInfo() : GalleryInfoImpl() {
    @JvmField
    @ColumnInfo(name = "STATE")
    var state = 0

    @JvmField
    @ColumnInfo(name = "LEGACY")
    var legacy = 0

    @JvmField
    @ColumnInfo(name = "TIME")
    var time: Long = 0

    @JvmField
    @ColumnInfo(name = "LABEL")
    var label: String? = null

    @JvmField
    @Ignore
    var speed: Long = 0

    @JvmField
    @Ignore
    var remaining: Long = 0

    @JvmField
    @Ignore
    var finished = 0

    @JvmField
    @Ignore
    var downloaded = 0

    @JvmField
    @Ignore
    var total = 0

    constructor(galleryInfo: GalleryInfo) : this() {
        gid = galleryInfo.gid
        token = galleryInfo.token
        title = galleryInfo.title
        titleJpn = galleryInfo.titleJpn
        thumb = galleryInfo.thumb
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