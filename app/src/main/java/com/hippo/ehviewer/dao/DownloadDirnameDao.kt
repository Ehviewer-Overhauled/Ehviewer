package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadDirnameDao {
    @Query("SELECT * FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    suspend fun load(gid: Long): DownloadDirname?

    @Update
    suspend fun update(downloadDirname: DownloadDirname)

    @Insert
    suspend fun insert(t: DownloadDirname): Long

    @Query("DELETE FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Query("SELECT * FROM DOWNLOAD_DIRNAME")
    suspend fun list(): List<DownloadDirname>
}
