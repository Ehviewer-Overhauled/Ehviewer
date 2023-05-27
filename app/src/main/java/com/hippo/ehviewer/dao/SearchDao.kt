package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SearchDao {
    @Query("DELETE FROM suggestions WHERE query = :query")
    fun deleteQuery(query: String)

    @Query("SELECT * FROM suggestions")
    fun rawSuggestions(): Array<Search>

    @Insert
    fun insert(search: Search)

    fun addQuery(query: String) {
        deleteQuery(query)
        val search = Search(System.currentTimeMillis(), query)
        insert(search)
    }

    fun suggestions(prefix: String, limit: Int) = rawSuggestions().map { it.query }.toTypedArray()
}
