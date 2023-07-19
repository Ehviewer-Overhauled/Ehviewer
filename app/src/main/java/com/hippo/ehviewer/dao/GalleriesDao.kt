package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GalleryDao {
    @Query("SELECT * FROM GALLERIES WHERE GID = :gid")
    suspend fun load(gid: Long): CommonGalleryInfo?

    @Query("SELECT * FROM GALLERIES")
    suspend fun list(): List<CommonGalleryInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(galleryInfoList: List<CommonGalleryInfo>)

    @Update
    suspend fun update(galleryInfo: CommonGalleryInfo)
}
