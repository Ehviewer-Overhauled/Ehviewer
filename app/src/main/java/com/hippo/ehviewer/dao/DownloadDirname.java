package com.hippo.ehviewer.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DOWNLOAD_DIRNAME")
public class DownloadDirname {
    @PrimaryKey
    @ColumnInfo(name = "GID")
    private long gid;
    @ColumnInfo(name = "DIRNAME")
    private String dirname;

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public String getDirname() {
        return dirname;
    }

    public void setDirname(String dirname) {
        this.dirname = dirname;
    }
}
