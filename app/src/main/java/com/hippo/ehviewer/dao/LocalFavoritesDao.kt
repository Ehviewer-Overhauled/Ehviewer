package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Upsert
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalFavoritesDao {
    @Query("SELECT COUNT(*) FROM LOCAL_FAVORITES")
    fun count(): Flow<Int>

    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    suspend fun list(): List<LocalFavoriteInfo>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun joinListLazy(): PagingSource<Int, BaseGalleryInfo>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID) WHERE TITLE LIKE :title ORDER BY TIME DESC")
    fun joinListLazy(title: String): PagingSource<Int, BaseGalleryInfo>

    @Query("SELECT EXISTS(SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid)")
    suspend fun contains(gid: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(localFavorites: List<LocalFavoriteInfo>)

    @Upsert
    suspend fun upsert(t: LocalFavoriteInfo)

    @Query("DELETE FROM LOCAL_FAVORITES WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)
}
