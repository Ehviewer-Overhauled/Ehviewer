package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Entity(tableName = "GALLERIES")
data class CommonGalleryInfo(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    override var gid: Long,

    @ColumnInfo(name = "TOKEN")
    override var token: String?,

    @ColumnInfo(name = "TITLE")
    override var title: String?,

    @ColumnInfo(name = "TITLE_JPN")
    override var titleJpn: String?,

    @ColumnInfo(name = "THUMB")
    override var thumbKey: String?,

    @ColumnInfo(name = "CATEGORY")
    override var category: Int = 0,

    @ColumnInfo(name = "POSTED")
    override var posted: String?,

    @ColumnInfo(name = "UPLOADER")
    override var uploader: String?,

    @ColumnInfo(name = "RATING")
    override var rating: Float,

    @ColumnInfo(name = "SIMPLE_LANGUAGE")
    override var simpleLanguage: String?,

    @ColumnInfo(name = "FAVORITE_SLOT")
    override var favoriteSlot: Int,
) : BaseGalleryInfo()
