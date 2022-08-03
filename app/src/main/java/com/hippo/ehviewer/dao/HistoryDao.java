package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HistoryDao extends BasicDao<HistoryInfo> {
    @Query("SELECT * FROM HISTORY WHERE GID = :gid")
    HistoryInfo load(long gid);

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    List<HistoryInfo> list();

    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT :limit OFFSET :offset")
    List<HistoryInfo> list(int offset, int limit);

    @Update
    void update(HistoryInfo historyInfo);

    @Insert
    long insert(HistoryInfo historyInfo);

    @Delete
    void delete(HistoryInfo historyInfos);

    @Delete
    void delete(List<HistoryInfo> historyInfos);

    @Query("DELETE FROM HISTORY")
    void deleteAll();
}
