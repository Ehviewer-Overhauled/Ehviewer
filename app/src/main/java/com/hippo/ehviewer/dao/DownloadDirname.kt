package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DOWNLOAD_DIRNAME")
data class DownloadDirname(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    val gid: Long,

    @ColumnInfo(name = "DIRNAME")
    val dirname: String,
)
