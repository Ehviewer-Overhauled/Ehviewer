package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggestions")
data class Search(
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "query")
    val query: String,
    @PrimaryKey
    @ColumnInfo(name = "_id")
    val id: Int? = null,
)
