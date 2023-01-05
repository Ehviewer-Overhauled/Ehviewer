package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadLabelDao : BasicDao<DownloadLabel> {
    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY TIME ASC")
    override fun list(): List<DownloadLabel>

    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY TIME ASC LIMIT :limit OFFSET :offset")
    fun list(offset: Int, limit: Int): List<DownloadLabel>

    @Update
    fun update(downloadLabels: List<DownloadLabel>)

    @Update
    fun update(downloadLabel: DownloadLabel)

    @Insert
    override fun insert(t: DownloadLabel): Long

    @Delete
    fun delete(downloadLabel: DownloadLabel)
}