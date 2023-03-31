package com.hippo.ehviewer.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkInfo::class, DownloadInfo::class, DownloadLabel::class, DownloadDirname::class, Filter::class, HistoryInfo::class, LocalFavoriteInfo::class, QuickSearch::class],
    version = 4,
    exportSchema = false,
)
abstract class EhDatabase : RoomDatabase() {
    abstract fun bookmarksBao(): BookmarksDao
    abstract fun downloadDirnameDao(): DownloadDirnameDao
    abstract fun downloadLabelDao(): DownloadLabelDao
    abstract fun downloadsDao(): DownloadsDao
    abstract fun filterDao(): FilterDao
    abstract fun historyDao(): HistoryDao
    abstract fun localFavoritesDao(): LocalFavoritesDao
    abstract fun quickSearchDao(): QuickSearchDao
}

fun buildMainDB(context: Context): EhDatabase {
    // TODO: Remove allowMainThreadQueries
    return Room.databaseBuilder(context, EhDatabase::class.java, "eh.db").allowMainThreadQueries()
        .build()
}
