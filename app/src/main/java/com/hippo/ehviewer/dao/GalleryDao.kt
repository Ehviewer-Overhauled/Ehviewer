package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Dao
interface GalleryDao {
    @Query("SELECT * FROM GALLERIES WHERE GID = :gid")
    suspend fun load(gid: Long): BaseGalleryInfo?

    @Query("SELECT * FROM GALLERIES")
    suspend fun list(): List<BaseGalleryInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(galleryInfoList: List<BaseGalleryInfo>)

    @Update
    suspend fun update(galleryInfo: BaseGalleryInfo)

    @Upsert
    suspend fun upsert(galleryInfo: BaseGalleryInfo)

    @Delete
    suspend fun delete(galleryInfo: BaseGalleryInfo)

    @Query("DELETE FROM GALLERIES WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)
}
