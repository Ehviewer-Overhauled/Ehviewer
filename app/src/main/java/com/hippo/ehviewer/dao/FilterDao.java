package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FilterDao extends BasicDao<Filter> {
    @Query("SELECT * FROM FILTER")
    List<Filter> list();

    @Update
    void update(Filter filter);

    @Insert
    long insert(Filter filter);

    @Delete
    void delete(Filter filter);

    @Query("SELECT * FROM FILTER")
    List<Filter> fakeList();

    @Insert
    void fakeInsert(Filter t);
}
