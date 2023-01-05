package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadDirnameDao : BasicDao<DownloadDirname> {
    @Query("SELECT * FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    fun load(gid: Long): DownloadDirname?

    @Update
    fun update(downloadDirname: DownloadDirname)

    @Insert
    override fun insert(t: DownloadDirname): Long

    @Query("DELETE FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    fun deleteByKey(gid: Long)

    @Query("DELETE FROM DOWNLOAD_DIRNAME")
    fun deleteAll()

    @Query("SELECT * FROM DOWNLOAD_DIRNAME")
    override fun list(): List<DownloadDirname>
}