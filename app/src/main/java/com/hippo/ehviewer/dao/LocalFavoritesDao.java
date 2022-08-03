package com.hippo.ehviewer.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocalFavoritesDao extends BasicDao<LocalFavoriteInfo> {
    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    List<LocalFavoriteInfo> list();

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE TITLE LIKE :title ORDER BY TIME DESC")
    List<LocalFavoriteInfo> list(String title);

    @Query("SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid")
    LocalFavoriteInfo load(long gid);

    @Insert
    long insert(LocalFavoriteInfo localFavoriteInfo);

    @Delete
    void delete(LocalFavoriteInfo localFavoriteInfo);

    @Query("DELETE FROM LOCAL_FAVORITES WHERE GID = :gid")
    void deleteByKey(long gid);

    @Query("SELECT * FROM LOCAL_FAVORITES ORDER BY TIME DESC")
    List<LocalFavoriteInfo> fakeList();

    @Insert
    void fakeInsert(LocalFavoriteInfo t);
}
