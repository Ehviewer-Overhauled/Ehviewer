package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DownloadsDao extends BasicDao<DownloadInfo> {
    @Query("SELECT * FROM DOWNLOADS ORDER BY TIME DESC")
    List<DownloadInfo> list();

    @Query("SELECT * FROM DOWNLOADS WHERE GID = :gid")
    DownloadInfo load(long gid);

    @Update
    void update(DownloadInfo downloadInfo);

    @Insert
    long insert(DownloadInfo downloadInfo);

    @Delete
    void delete(DownloadInfo downloadInfo);

    @Query("SELECT * FROM DOWNLOADS ORDER BY TIME DESC")
    List<DownloadInfo> fakeList();

    @Insert
    void fakeInsert(DownloadInfo t);
}
