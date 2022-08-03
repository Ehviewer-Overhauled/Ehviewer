package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuickSearchDao extends BasicDao<QuickSearch> {
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

    @Query("SELECT * FROM QUICK_SEARCH ORDER BY TIME ASC")
    List<QuickSearch> fakeList();

    @Insert
    void fakeInsert(QuickSearch t);
}
