package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FILTER")
data class Filter(
    @JvmField
    @ColumnInfo(name = "MODE")
    var mode: Int = 0,

    @JvmField
    @ColumnInfo(name = "TEXT")
    var text: String? = null,

    @JvmField
    @ColumnInfo(name = "ENABLE")
    var enable: Boolean? = null,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null
)