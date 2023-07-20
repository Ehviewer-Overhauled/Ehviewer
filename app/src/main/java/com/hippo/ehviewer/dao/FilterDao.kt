package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FilterDao {
    @Query("SELECT * FROM FILTER")
    suspend fun list(): List<Filter>

    @Update
    suspend fun update(filter: Filter)

    @Insert
    suspend fun insert(t: Filter): Long

    @Insert
    suspend fun insert(filters: List<Filter>)

    @Delete
    suspend fun delete(filter: Filter)

    @Query("SELECT * FROM FILTER WHERE TEXT = :text AND MODE = :mode")
    suspend fun load(text: String, mode: Int): Filter?
}
