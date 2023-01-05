package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hippo.util.HashCodeUtils

@Entity(tableName = "FILTER")
class Filter {
    @JvmField
    @ColumnInfo(name = "MODE")
    var mode = 0

    @JvmField
    @ColumnInfo(name = "TEXT")
    var text: String? = null

    @JvmField
    @ColumnInfo(name = "ENABLE")
    var enable: Boolean? = null

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null
    override fun hashCode(): Int {
        return HashCodeUtils.hashCode(mode, text)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Filter) {
            return false
        }
        return other.mode == mode && other.text == text
    }
}