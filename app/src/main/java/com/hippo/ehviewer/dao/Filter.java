package com.hippo.ehviewer.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.hippo.util.HashCodeUtils;
import com.hippo.yorozuya.ObjectUtils;

@Entity(tableName = "FILTER")
public class Filter {

    @ColumnInfo(name = "MODE")
    public int mode;
    @ColumnInfo(name = "TEXT")
    public String text;
    @ColumnInfo(name = "ENABLE")
    public Boolean enable;
    @PrimaryKey
    @ColumnInfo(name = "_id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hashCode(mode, text);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Filter)) {
            return false;
        }
        Filter filter = (Filter) o;
        return filter.mode == mode && ObjectUtils.equal(filter.text, text);
    }

}
