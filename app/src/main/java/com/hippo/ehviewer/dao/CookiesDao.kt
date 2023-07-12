package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CookiesDao {
    @Query("SELECT * FROM OK_HTTP_3_COOKIE")
    suspend fun list(): List<Cookie>
}
