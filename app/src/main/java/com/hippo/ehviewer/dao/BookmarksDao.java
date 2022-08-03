package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookmarksDao extends BasicDao<BookmarkInfo> {
    @Insert
    long insert(BookmarkInfo bookmark);

    @Delete
    void delete(BookmarkInfo bookmark);

    @Query("SELECT * FROM BOOKMARKS ORDER BY TIME DESC")
    List<BookmarkInfo> list();
}
