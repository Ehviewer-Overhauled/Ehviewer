package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadLabelDao {
    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY POSITION ASC")
    suspend fun list(): List<DownloadLabel>

    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY POSITION ASC LIMIT :limit OFFSET :offset")
    suspend fun list(offset: Int, limit: Int): List<DownloadLabel>

    @Query("UPDATE DOWNLOAD_LABELS SET POSITION = POSITION - 1 WHERE POSITION > :position")
    suspend fun fill(position: Int)

    @Update
    suspend fun update(downloadLabels: List<DownloadLabel>)

    @Update
    suspend fun update(downloadLabel: DownloadLabel)

    @Insert
    suspend fun insert(downloadLabel: DownloadLabel): Long

    @Insert
    suspend fun insert(downloadLabels: List<DownloadLabel>)

    @Delete
    suspend fun delete(downloadLabel: DownloadLabel)
}
