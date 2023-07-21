package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface DownloadsDao {
    @Query("SELECT * FROM DOWNLOADS ORDER BY POSITION DESC")
    suspend fun list(): List<DownloadEntity>

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) ORDER BY POSITION DESC")
    suspend fun joinList(): List<DownloadInfo>

    @Query("UPDATE DOWNLOADS SET POSITION = POSITION - :offset WHERE POSITION > :position")
    suspend fun fill(position: Int, offset: Int = 1)

    @Update
    suspend fun update(downloadInfo: List<DownloadEntity>)

    @Insert
    suspend fun insert(downloadInfo: List<DownloadEntity>)

    @Upsert
    suspend fun upsert(t: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: List<DownloadEntity>)
}
