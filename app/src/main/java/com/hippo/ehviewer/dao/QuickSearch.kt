package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QUICK_SEARCH")
class QuickSearch {
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null

    @ColumnInfo(name = "NAME")
    var name: String? = null

    @ColumnInfo(name = "MODE")
    var mode = 0

    @ColumnInfo(name = "CATEGORY")
    var category = 0

    @ColumnInfo(name = "KEYWORD")
    var keyword: String? = null

    @ColumnInfo(name = "ADVANCE_SEARCH")
    var advanceSearch = 0

    @ColumnInfo(name = "MIN_RATING")
    var minRating = 0

    @ColumnInfo(name = "PAGE_FROM")
    var pageFrom = 0

    @ColumnInfo(name = "PAGE_TO")
    var pageTo = 0

    @ColumnInfo(name = "TIME")
    var time: Long = 0
    override fun toString(): String {
        return name!!
    }
}