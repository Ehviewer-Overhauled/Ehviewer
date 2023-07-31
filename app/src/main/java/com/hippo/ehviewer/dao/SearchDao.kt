package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SearchDao {
    @Query("DELETE FROM suggestions")
    suspend fun clear()

    @Query("DELETE FROM suggestions WHERE `query` = :query")
    suspend fun deleteQuery(query: String)

    @Query("SELECT * FROM suggestions WHERE `query` LIKE :prefix || '%' ORDER BY date DESC LIMIT :limit")
    suspend fun rawSuggestions(prefix: String, limit: Int): Array<Search>

    @Query("SELECT * FROM suggestions ORDER BY date DESC LIMIT :limit")
    suspend fun list(limit: Int): Array<Search>

    @Insert
    suspend fun insert(search: Search)
}
