package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "DOWNLOAD_LABELS", indices = [Index("LABEL", unique = true)])
data class DownloadLabel(
    @ColumnInfo(name = "LABEL")
    var label: String,

    @ColumnInfo(name = "POSITION")
    var position: Int,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
)
