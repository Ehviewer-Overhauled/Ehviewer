package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SearchDao {
    @Query("DELETE FROM suggestions WHERE `query` = :query")
    fun deleteQuery(query: String)

    @Query("SELECT * FROM suggestions WHERE `query` LIKE :prefix || '%' ORDER BY date DESC LIMIT :limit")
    fun rawSuggestions(prefix: String, limit: Int): Array<Search>

    @Query("SELECT * FROM suggestions ORDER BY date DESC LIMIT :limit")
    fun list(limit: Int): Array<Search>

    @Insert
    fun insert(search: Search)

    fun addQuery(query: String) {
        deleteQuery(query)
        val search = Search(System.currentTimeMillis(), query)
        insert(search)
    }

    fun suggestions(prefix: String, limit: Int) = (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit)).map { it.query }
}
