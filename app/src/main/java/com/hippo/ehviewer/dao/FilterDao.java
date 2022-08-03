package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FilterDao {

    String TABLENAME = "FILTER";

    @Query("SELECT * FROM FILTER")
    List<Filter> list();

    @Update
    void update(Filter filter);

    @Insert
    long insert(Filter filter);

    @Delete
    void delete(Filter filter);

}
