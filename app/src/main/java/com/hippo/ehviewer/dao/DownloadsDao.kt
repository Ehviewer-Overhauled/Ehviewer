package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadsDao : BasicDao<DownloadInfo> {
    @Query("SELECT * FROM DOWNLOADS ORDER BY TIME DESC")
    override fun list(): List<DownloadInfo>

    @Query("SELECT * FROM DOWNLOADS WHERE GID = :gid")
    fun load(gid: Long): DownloadInfo?

    @Update
    fun update(downloadInfo: DownloadInfo)

    @Insert
    override fun insert(t: DownloadInfo): Long

    @Delete
    fun delete(downloadInfo: DownloadInfo)
}