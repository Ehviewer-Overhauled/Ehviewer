package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DownloadLabelDao extends BasicDao<DownloadLabel> {
    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY TIME ASC")
    List<DownloadLabel> list();

    @Query("SELECT * FROM DOWNLOAD_LABELS ORDER BY TIME ASC LIMIT :limit OFFSET :offset")
    List<DownloadLabel> list(int offset, int limit);

    @Update
    void update(List<DownloadLabel> downloadLabels);

    @Update
    void update(DownloadLabel downloadLabel);

    @Insert
    long insert(DownloadLabel downloadLabel);

    @Delete
    void delete(DownloadLabel downloadLabel);
}
