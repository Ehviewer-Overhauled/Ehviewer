package com.hippo.ehviewer.dao

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED

@DeleteTable(tableName = "BOOKMARKS")
class DropBookMarkDao : AutoMigrationSpec

@RenameColumn(tableName = "HISTORY", fromColumnName = "MODE", toColumnName = "FAVORITE_SLOT")
class HistoryMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE HISTORY SET FAVORITE_SLOT = FAVORITE_SLOT - 2")
    }
}

class AddPositionColumn : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "QUICK_SEARCH",
            "DOWNLOAD_LABELS",
            "DOWNLOADS",
        )
        needMigrationTables.forEach { table ->
            // TODO: Rewrite this with row_number() when min sdk is 30 (SQLite 3.28.0)
            db.execSQL("UPDATE $table SET POSITION = (SELECT COUNT(*) FROM $table T WHERE T.TIME < $table.TIME)")
        }
    }
}

@DeleteColumn(tableName = "QUICK_SEARCH", columnName = "TIME")
@DeleteColumn(tableName = "DOWNLOAD_LABELS", columnName = "TIME")
class DropTimeColumn : AutoMigrationSpec

class AddGalleryTable : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "HISTORY",
            "DOWNLOADS",
            "LOCAL_FAVORITES",
        )
        needMigrationTables.forEachIndexed { index, table ->
            db.execSQL(
                "INSERT OR IGNORE INTO GALLERIES " +
                    "SELECT GID, TOKEN, TITLE, TITLE_JPN, THUMB, CATEGORY, POSTED, UPLOADER, RATING, SIMPLE_LANGUAGE, " +
                    "${if (index == 0) "FAVORITE_SLOT" else NOT_FAVORITED} FROM $table",
            )
        }
    }
}

@DeleteColumn(tableName = "HISTORY", columnName = "TOKEN")
@DeleteColumn(tableName = "HISTORY", columnName = "TITLE")
@DeleteColumn(tableName = "HISTORY", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "HISTORY", columnName = "THUMB")
@DeleteColumn(tableName = "HISTORY", columnName = "CATEGORY")
@DeleteColumn(tableName = "HISTORY", columnName = "POSTED")
@DeleteColumn(tableName = "HISTORY", columnName = "UPLOADER")
@DeleteColumn(tableName = "HISTORY", columnName = "RATING")
@DeleteColumn(tableName = "HISTORY", columnName = "SIMPLE_LANGUAGE")
@DeleteColumn(tableName = "HISTORY", columnName = "FAVORITE_SLOT")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TOKEN")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TITLE")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "THUMB")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "CATEGORY")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "POSTED")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "UPLOADER")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "RATING")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "SIMPLE_LANGUAGE")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TOKEN")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TITLE")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "THUMB")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "CATEGORY")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "POSTED")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "UPLOADER")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "RATING")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "SIMPLE_LANGUAGE")
class GalleryTableMigration : AutoMigrationSpec

class ThumbKeyMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
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
    entities = [BaseGalleryInfo::class, DownloadLabel::class, DownloadEntity::class, DownloadDirname::class, Filter::class, HistoryInfo::class, LocalFavoriteInfo::class, QuickSearch::class],
    version = 16,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 4,
            to = 5,
            spec = ThumbKeyMigration::class,
        ),
        AutoMigration(
            from = 5,
            to = 6,
        ),
        AutoMigration(
            from = 6,
            to = 7,
            spec = DropBookMarkDao::class,
        ),
        AutoMigration(
            from = 7,
            to = 8,
        ),
        AutoMigration(
            from = 8,
            to = 9,
        ),
        AutoMigration(
            from = 9,
            to = 10,
            spec = HistoryMigration::class,
        ),
        AutoMigration(
            from = 10,
            to = 11,
            spec = AddPositionColumn::class,
        ),
        AutoMigration(
            from = 11,
            to = 12,
            spec = DropTimeColumn::class,
        ),
        AutoMigration(
            from = 12,
            to = 13,
            spec = AddGalleryTable::class,
        ),
        AutoMigration(
            from = 13,
            to = 14,
            spec = GalleryTableMigration::class,
        ),
        AutoMigration(
            from = 14,
            to = 15,
        ),
        AutoMigration(
            from = 15,
            to = 16,
        ),
    ],
)
@TypeConverters(FilterModeConverter::class)
abstract class EhDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao
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

@Database(
    entities = [Search::class],
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        ),
    ],
)
abstract class SearchDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao
}
