package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuickSearchDao {

    String TABLENAME = "QUICK_SEARCH";

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC")
    List<QuickSearch> list();

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC LIMIT :limit OFFSET :offset")
    List<QuickSearch> list(int offset, int limit);

    @Update
    void update(List<QuickSearch> downloadLabels);

    @Update
    void update(QuickSearch quickSearch);

    @Insert
    long insert(QuickSearch quickSearch);

    @Delete
    void delete(QuickSearch quickSearch);

}
