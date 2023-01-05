package com.hippo.ehviewer.dao;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {BookmarkInfo.class, DownloadInfo.class, DownloadLabel.class, DownloadDirname.class, Filter.class, HistoryInfo.class, LocalFavoriteInfo.class, QuickSearch.class}, version = 4, exportSchema = false)
public abstract class EhDatabase extends RoomDatabase {
    public abstract BookmarksDao bookmarksBao();

    public abstract DownloadDirnameDao downloadDirnameDao();

    public abstract DownloadLabelDao downloadLabelDao();

    public abstract DownloadsDao downloadsDao();

    public abstract FilterDao filterDao();

    public abstract HistoryDao historyDao();

    public abstract LocalFavoritesDao localFavoritesDao();

    public abstract QuickSearchDao quickSearchDao();
}
