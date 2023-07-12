package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuickSearchDao {
    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC")
    suspend fun list(): List<QuickSearch>

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC LIMIT :limit OFFSET :offset")
    suspend fun list(offset: Int, limit: Int): List<QuickSearch>

    @Update
    suspend fun update(downloadLabels: List<QuickSearch>)

    @Update
    suspend fun update(quickSearch: QuickSearch)

    @Insert
    suspend fun insert(t: QuickSearch): Long

    @Delete
    suspend fun delete(quickSearch: QuickSearch)
}
