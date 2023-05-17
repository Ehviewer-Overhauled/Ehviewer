package com.hippo.ehviewer.dao

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val EHDB_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "DOWNLOADS",
            "HISTORY",
            "LOCAL_FAVORITES",
        )
        val prefixToRemove = arrayOf(
            "https://ehgt.org/",
            "https://s.exhentai.org/t/",
            "https://exhentai.org/t/",
        )
        needMigrationTables.forEach { table ->
            prefixToRemove.forEach { prefix ->
                db.execSQL("UPDATE $table SET thumb = SUBSTR(thumb ,LENGTH('$prefix') + 1) WHERE thumb LIKE '$prefix%'")
            }
        }
    }
}

@Database(
    entities = [BookmarkInfo::class, DownloadInfo::class, DownloadLabel::class, DownloadDirname::class, Filter::class, HistoryInfo::class, LocalFavoriteInfo::class, QuickSearch::class],
    version = 5,
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

// 1 -> 2 some nullability changes
@Database(
    entities = [Cookie::class],
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ),
    ],
)
abstract class CookiesDatabase : RoomDatabase() {
    abstract fun cookiesDao(): CookiesDao
}

fun buildMainDB(context: Context): EhDatabase {
    // TODO: Remove allowMainThreadQueries
    return Room.databaseBuilder(context, EhDatabase::class.java, "eh.db").allowMainThreadQueries()
        .addMigrations(EHDB_MIGRATION_4_5)
        .build()
}

fun buildCookiesDB(context: Context): CookiesDatabase {
    return Room.databaseBuilder(context, CookiesDatabase::class.java, "okhttp3-cookie.db").build()
}
