package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo

@Entity(tableName = "HISTORY")
class HistoryInfo() : BaseGalleryInfo() {
    @JvmField
    @ColumnInfo(name = "TIME")
    var time: Long = 0

    // Trick: Use MODE for favoriteSlot
    // Shadow its accessors
    @ColumnInfo(name = "MODE")
    private val mode: Int = 0

    fun getMode(): Int {
        return favoriteSlot + 2
    }

    fun setMode(mode: Int) {
        favoriteSlot = mode - 2
    }
    // Trick end

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
        favoriteSlot = galleryInfo.favoriteSlot
    }
}