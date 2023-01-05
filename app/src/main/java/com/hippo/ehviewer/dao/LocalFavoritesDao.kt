package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocalFavoritesDao : BasicDao<LocalFavoriteInfo> {
    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    override fun list(): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE TITLE LIKE :title ORDER BY TIME DESC")
    fun list(title: String): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid")
    fun load(gid: Long): LocalFavoriteInfo?

    @Insert
    override fun insert(t: LocalFavoriteInfo): Long

    @Delete
    fun delete(localFavoriteInfo: LocalFavoriteInfo)

    @Query("DELETE FROM LOCAL_FAVORITES WHERE GID = :gid")
    fun deleteByKey(gid: Long)
}