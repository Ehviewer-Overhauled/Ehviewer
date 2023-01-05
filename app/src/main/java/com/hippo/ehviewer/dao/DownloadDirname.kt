package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DOWNLOAD_DIRNAME")
class DownloadDirname {
    @PrimaryKey
    @ColumnInfo(name = "GID")
    var gid: Long = 0

    @ColumnInfo(name = "DIRNAME")
    var dirname: String? = null
}