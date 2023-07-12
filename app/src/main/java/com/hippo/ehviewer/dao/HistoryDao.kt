package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HistoryDao {
    @Query("SELECT * FROM HISTORY WHERE GID = :gid")
    suspend fun load(gid: Long): HistoryInfo?

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    suspend fun list(): List<HistoryInfo>

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    fun listLazy(): PagingSource<Int, HistoryInfo>

    @Update
    suspend fun update(historyInfo: HistoryInfo)

    @Insert
    suspend fun insert(t: HistoryInfo): Long

    @Delete
    suspend fun delete(historyInfo: HistoryInfo)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAll()
}
