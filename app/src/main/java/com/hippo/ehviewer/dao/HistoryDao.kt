package com.hippo.ehviewer.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Upsert
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Dao
interface HistoryDao {
    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    suspend fun list(): List<HistoryInfo>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM HISTORY JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun joinListLazy(): PagingSource<Int, BaseGalleryInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(historyInfoList: List<HistoryInfo>)

    @Upsert
    suspend fun upsert(historyInfo: HistoryInfo)

    @Query("DELETE FROM HISTORY WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAll()
}
