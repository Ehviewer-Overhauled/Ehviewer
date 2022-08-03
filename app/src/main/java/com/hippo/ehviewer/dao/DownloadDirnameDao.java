package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DownloadDirnameDao {

    @Query("SELECT * FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    DownloadDirname load(long gid);

    @Update
    void update(DownloadDirname downloadDirname);

    @Insert
    void insert(DownloadDirname downloadDirname);

    @Query("DELETE FROM DOWNLOAD_DIRNAME WHERE GID = :gid")
    void deleteByKey(long gid);

    @Query("DELETE FROM DOWNLOAD_DIRNAME")
    void deleteAll();
}
