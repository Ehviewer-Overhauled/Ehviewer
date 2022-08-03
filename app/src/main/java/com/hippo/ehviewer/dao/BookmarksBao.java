package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;

@Dao
public interface BookmarksBao {

    @Insert
    void insert(BookmarkInfo bookmark);

    @Delete
    void delete(BookmarkInfo bookmark);

}
