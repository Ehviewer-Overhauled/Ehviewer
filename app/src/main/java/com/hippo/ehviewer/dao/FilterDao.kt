package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FilterDao : BasicDao<Filter> {
    @Query("SELECT * FROM FILTER")
    override fun list(): List<Filter>

    @Update
    fun update(filter: Filter)

    @Insert
    override fun insert(t: Filter): Long

    @Delete
    fun delete(filter: Filter)

    @Query("SELECT * FROM FILTER WHERE TEXT = :text AND MODE = :mode")
    fun load(text: String, mode: Int): Filter?
}