package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HistoryDao : BasicDao<HistoryInfo> {
    @Query("SELECT * FROM HISTORY WHERE GID = :gid")
    fun load(gid: Long): HistoryInfo?

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    override fun list(): List<HistoryInfo>

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT :limit OFFSET :offset")
    fun list(offset: Int, limit: Int): List<HistoryInfo>

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    fun listLazy(): PagingSource<Int, HistoryInfo>

    @Update
    fun update(historyInfo: HistoryInfo)

    @Insert
    override fun insert(t: HistoryInfo): Long

    @Delete
    fun delete(historyInfo: HistoryInfo)

    @Delete
    fun delete(historyInfo: List<HistoryInfo>)

    @Query("DELETE FROM HISTORY")
    fun deleteAll()
}