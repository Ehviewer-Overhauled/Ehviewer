package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuickSearchDao : BasicDao<QuickSearch> {
    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC")
    override fun list(): List<QuickSearch>

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC LIMIT :limit OFFSET :offset")
    fun list(offset: Int, limit: Int): List<QuickSearch>

    @Update
    fun update(downloadLabels: List<QuickSearch>)

    @Update
    fun update(quickSearch: QuickSearch)

    @Insert
    override fun insert(t: QuickSearch): Long

    @Delete
    fun delete(quickSearch: QuickSearch?)
}