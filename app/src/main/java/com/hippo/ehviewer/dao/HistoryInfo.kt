package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED

@Entity(tableName = "HISTORY")
class HistoryInfo() : BaseGalleryInfo() {
    @ColumnInfo(name = "TIME")
    var time: Long = 0

    @ColumnInfo(name = "FAVORITE_SLOT")
    override var favoriteSlot: Int = NOT_FAVORITED

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
        favoriteSlot = galleryInfo.favoriteSlot
    }
}
