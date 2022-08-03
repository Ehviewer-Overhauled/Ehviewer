package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DownloadDirnameDao extends BasicDao<DownloadDirname> {
    @Query("SELECT * FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    DownloadDirname load(long gid);

    @Update
    void update(DownloadDirname downloadDirname);

    @Insert
    long insert(DownloadDirname downloadDirname);

    @Query("DELETE FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    void deleteByKey(long gid);

    @Query("DELETE FROM DOWNLOAD_DIRNAME")
    void deleteAll();

    @Query("SELECT * FROM DOWNLOAD_DIRNAME")
    List<DownloadDirname> list();
}
