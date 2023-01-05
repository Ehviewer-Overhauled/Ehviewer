package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BookmarksDao : BasicDao<BookmarkInfo> {
    @Insert
    override fun insert(t: BookmarkInfo): Long

    @Delete
    fun delete(bookmark: BookmarkInfo)

    @Query("SELECT * FROM BOOKMARKS ORDER BY TIME DESC")
    override fun list(): List<BookmarkInfo>
}