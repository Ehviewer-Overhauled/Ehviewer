package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "FILTER")
data class Filter(
    @ColumnInfo(name = "MODE")
    val mode: FilterMode,

    @ColumnInfo(name = "TEXT")
    var text: String,

    @ColumnInfo(name = "ENABLE")
    var enable: Boolean = true,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
)

enum class FilterMode(val field: Int) {
    TITLE(0),
    UPLOADER(1),
    TAG(2),
    TAG_NAMESPACE(3),
    COMMENTER(4),
    COMMENT(5),
}

class FilterModeConverter {
    @TypeConverter
    fun fromFilterMode(mode: FilterMode) = mode.field

    @TypeConverter
    fun toFilterMode(mode: Int) = FilterMode.entries[mode]
}
