package com.hippo.ehviewer.dao;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "QUICK_SEARCH")
public class QuickSearch {
    @PrimaryKey
    @ColumnInfo(name = "_id")
    public Long id;
    @ColumnInfo(name = "NAME")
    public String name;
    @ColumnInfo(name = "MODE")
    public int mode;
    @ColumnInfo(name = "CATEGORY")
    public int category;
    @ColumnInfo(name = "KEYWORD")
    public String keyword;
    @ColumnInfo(name = "ADVANCE_SEARCH")
    public int advanceSearch;
    @ColumnInfo(name = "MIN_RATING")
    public int minRating;
    @ColumnInfo(name = "PAGE_FROM")
    public int pageFrom;
    @ColumnInfo(name = "PAGE_TO")
    public int pageTo;
    @ColumnInfo(name = "TIME")
    public long time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getAdvanceSearch() {
        return advanceSearch;
    }

    public void setAdvanceSearch(int advanceSearch) {
        this.advanceSearch = advanceSearch;
    }

    public int getMinRating() {
        return minRating;
    }

    public void setMinRating(int minRating) {
        this.minRating = minRating;
    }

    public int getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(int pageFrom) {
        this.pageFrom = pageFrom;
    }

    public int getPageTo() {
        return pageTo;
    }

    public void setPageTo(int pageTo) {
        this.pageTo = pageTo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
