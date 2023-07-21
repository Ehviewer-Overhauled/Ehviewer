package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DownloadDirnameDao {
    @Query("SELECT * FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    suspend fun load(gid: Long): DownloadDirname?

    @Upsert
    suspend fun upsert(downloadDirname: DownloadDirname)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(downloadDirnameList: List<DownloadDirname>)

    @Query("DELETE FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Query("SELECT * FROM DOWNLOAD_DIRNAME")
    suspend fun list(): List<DownloadDirname>
}
