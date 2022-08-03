package com.hippo.ehviewer.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DOWNLOAD_LABELS")
public class DownloadLabel {

    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;
    @ColumnInfo(name = "LABEL")
    private String label;
    @ColumnInfo(name = "TIME")
    private long time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

}
