package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CookiesDao {
    @Query("SELECT * FROM OK_HTTP_3_COOKIE")
    fun list(): List<Cookie>

    @Delete
    fun delete(cookie: Cookie)

    @Insert
    fun insert(cookie: Cookie): Long

    @Update
    fun update(cookie: Cookie)
}
