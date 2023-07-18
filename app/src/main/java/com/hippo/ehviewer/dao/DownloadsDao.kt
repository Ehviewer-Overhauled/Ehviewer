package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadsDao {
    @Query("SELECT * FROM DOWNLOADS ORDER BY POSITION DESC")
    suspend fun list(): List<DownloadInfo>

    @Query("SELECT * FROM DOWNLOADS ORDER BY POSITION DESC LIMIT :limit OFFSET :offset")
    suspend fun list(offset: Int, limit: Int): List<DownloadInfo>

    @Query("SELECT * FROM DOWNLOADS WHERE GID = :gid")
    suspend fun load(gid: Long): DownloadInfo?

    @Query("UPDATE DOWNLOADS SET POSITION = POSITION - :offset WHERE POSITION > :position")
    suspend fun fill(position: Int, offset: Int = 1)

    @Update
    suspend fun update(downloadInfo: List<DownloadInfo>)

    @Update
    suspend fun update(downloadInfo: DownloadInfo)

    @Insert
    suspend fun insert(t: DownloadInfo): Long

    @Delete
    suspend fun delete(downloadInfo: DownloadInfo)

    @Delete
    suspend fun delete(downloadInfo: List<DownloadInfo>)
}
