package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.hippo.ehviewer.client.data.AbstractGalleryInfo
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Entity(tableName = "LOCAL_FAVORITES")
class LocalFavoriteInfo(
    @Ignore
    val galleryInfo: GalleryInfo = BaseGalleryInfo(),
) : BaseGalleryInfo(), AbstractGalleryInfo by galleryInfo {
    constructor() : this(galleryInfo = BaseGalleryInfo())

    @ColumnInfo(name = "TIME")
    var time: Long = 0

    @Ignore
    override var favoriteSlot = LOCAL_FAVORITED
}
