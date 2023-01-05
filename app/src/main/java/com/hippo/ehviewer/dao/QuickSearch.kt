package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QUICK_SEARCH")
data class QuickSearch(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,

    @ColumnInfo(name = "NAME")
    var name: String? = null,

    @ColumnInfo(name = "MODE")
    var mode: Int = 0,

    @ColumnInfo(name = "CATEGORY")
    var category: Int = 0,

    @ColumnInfo(name = "KEYWORD")
    var keyword: String? = null,

    @ColumnInfo(name = "ADVANCE_SEARCH")
    var advanceSearch: Int = 0,

    @ColumnInfo(name = "MIN_RATING")
    var minRating: Int = 0,

    @ColumnInfo(name = "PAGE_FROM")
    var pageFrom: Int = 0,

    @ColumnInfo(name = "PAGE_TO")
    var pageTo: Int = 0,

    @ColumnInfo(name = "TIME")
    var time: Long = 0
)