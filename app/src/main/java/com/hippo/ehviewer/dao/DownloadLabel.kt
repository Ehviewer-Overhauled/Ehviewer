package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "DOWNLOAD_LABELS")
data class DownloadLabel(
    @ColumnInfo(name = "LABEL")
    var label: String,

    @ColumnInfo(name = "TIME")
    var time: Long = Instant.now().toEpochMilli(),

    @ColumnInfo(name = "POSITION", defaultValue = "0")
    var position: Int,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
)
