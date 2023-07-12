package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocalFavoritesDao {
    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    suspend fun list(): List<LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    fun listLazy(): PagingSource<Int, LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE TITLE LIKE :title ORDER BY TIME DESC")
    fun listLazy(title: String): PagingSource<Int, LocalFavoriteInfo>

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid")
    suspend fun load(gid: Long): LocalFavoriteInfo?

    @Query("SELECT EXISTS(SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid)")
    suspend fun contains(gid: Long): Boolean

    @Insert
    suspend fun insert(t: LocalFavoriteInfo): Long

    @Delete
    suspend fun delete(localFavoriteInfo: LocalFavoriteInfo)

    @Query("DELETE FROM LOCAL_FAVORITES WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)
}
