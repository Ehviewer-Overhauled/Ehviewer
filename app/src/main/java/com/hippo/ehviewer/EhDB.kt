/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.Room.databaseBuilder
import com.hippo.ehviewer.EhApplication.Companion.ehDatabase
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.BasicDao
import com.hippo.ehviewer.dao.DownloadDirname
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.DownloadLabel
import com.hippo.ehviewer.dao.EhDatabase
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.dao.LocalFavoriteInfo
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.IOUtils
import com.hippo.yorozuya.ObjectUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object EhDB {
    private const val CUR_DB_VER = 4
    private val db = ehDatabase

    // Fix state
    @get:Synchronized
    val allDownloadInfo: List<DownloadInfo>
        get() {
            val dao = db.downloadsDao()
            val list = dao.list()
            // Fix state
            for (info in list) {
                if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                    info.state = DownloadInfo.STATE_NONE
                }
            }
            return list
        }

    @Synchronized
    fun updateDownloadInfo(downloadInfos: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfos)
    }

    // Insert or update
    @Synchronized
    fun putDownloadInfo(downloadInfo: DownloadInfo) {
        val dao = db.downloadsDao()
        if (null != dao.load(downloadInfo.gid)) {
            // Update
            dao.update(downloadInfo)
        } else {
            // Insert
            dao.insert(downloadInfo)
        }
    }

    @Synchronized
    fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        db.downloadsDao().delete(downloadInfo)
    }

    @Synchronized
    fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    /**
     * Insert or update
     */
    @Synchronized
    fun putDownloadDirname(gid: Long, dirname: String?) {
        val dao = db.downloadDirnameDao()
        var raw = dao.load(gid)
        if (raw != null) { // Update
            raw.dirname = dirname
            dao.update(raw)
        } else { // Insert
            raw = DownloadDirname()
            raw.gid = gid
            raw.dirname = dirname
            dao.insert(raw)
        }
    }

    @Synchronized
    fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    @Synchronized
    fun clearDownloadDirname() {
        val dao = db.downloadDirnameDao()
        dao.deleteAll()
    }

    @get:Synchronized
    val allDownloadLabelList: List<DownloadLabel>
        get() {
            val dao = db.downloadLabelDao()
            return dao.list()
        }

    @Synchronized
    fun addDownloadLabel(label: String?): DownloadLabel {
        val dao = db.downloadLabelDao()
        val raw = DownloadLabel()
        raw.label = label
        raw.time = System.currentTimeMillis()
        raw.id = dao.insert(raw)
        return raw
    }

    @Synchronized
    fun addDownloadLabel(raw: DownloadLabel): DownloadLabel {
        // Reset id
        raw.id = null
        val dao = db.downloadLabelDao()
        raw.id = dao.insert(raw)
        return raw
    }

    @Synchronized
    fun updateDownloadLabel(raw: DownloadLabel?) {
        val dao = db.downloadLabelDao()
        dao.update(raw!!)
    }

    @Synchronized
    fun moveDownloadLabel(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        val reverse = fromPosition > toPosition
        val offset = if (reverse) toPosition else fromPosition
        val limit = if (reverse) fromPosition - toPosition + 1 else toPosition - fromPosition + 1
        val dao = db.downloadLabelDao()
        val list = dao.list(offset, limit)
        val step = if (reverse) 1 else -1
        val start = if (reverse) limit - 1 else 0
        val end = if (reverse) 0 else limit - 1
        val toTime = list[end].time
        var i = end
        while (if (reverse) i < start else i > start) {
            list[i].time = list[i + step].time
            i += step
        }
        list[start].time = toTime
        dao.update(list)
    }

    @Synchronized
    fun removeDownloadLabel(raw: DownloadLabel?) {
        val dao = db.downloadLabelDao()
        dao.delete(raw!!)
    }

    @get:Synchronized
    val allLocalFavorites: List<GalleryInfo>
        get() {
            val dao = db.localFavoritesDao()
            val list = dao.list()
            return ArrayList<GalleryInfo>(list)
        }

    @Synchronized
    fun searchLocalFavorites(query: String): List<GalleryInfo> {
        val dao = db.localFavoritesDao()
        val list = dao.list("%$query%")
        return ArrayList<GalleryInfo>(list)
    }

    @Synchronized
    fun removeLocalFavorites(gid: Long) {
        db.localFavoritesDao().deleteByKey(gid)
    }

    @Synchronized
    fun removeLocalFavorites(gidArray: LongArray) {
        val dao = db.localFavoritesDao()
        for (gid in gidArray) {
            dao.deleteByKey(gid)
        }
    }

    @JvmStatic
    @Synchronized
    fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return null != dao.load(gid)
    }

    @Synchronized
    fun putLocalFavorites(galleryInfo: GalleryInfo) {
        val dao = db.localFavoritesDao()
        if (null == dao.load(galleryInfo.gid)) {
            val info: LocalFavoriteInfo
            if (galleryInfo is LocalFavoriteInfo) {
                info = galleryInfo
            } else {
                info = LocalFavoriteInfo(galleryInfo)
                info.time = System.currentTimeMillis()
            }
            dao.insert(info)
        }
    }

    @Synchronized
    fun putLocalFavorites(galleryInfoList: List<GalleryInfo>) {
        for (gi in galleryInfoList) {
            putLocalFavorites(gi)
        }
    }

    @get:Synchronized
    val allQuickSearch: List<QuickSearch>
        get() {
            val dao = db.quickSearchDao()
            return dao.list()
        }

    @Synchronized
    fun insertQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        quickSearch.id = null
        quickSearch.time = System.currentTimeMillis()
        quickSearch.id = dao.insert(quickSearch)
    }

    @Synchronized
    fun importQuickSearch(quickSearchList: List<QuickSearch?>) {
        val dao = db.quickSearchDao()
        for (quickSearch in quickSearchList) {
            dao.insert(quickSearch!!)
        }
    }

    @Synchronized
    fun deleteQuickSearch(quickSearch: QuickSearch?) {
        val dao = db.quickSearchDao()
        dao.delete(quickSearch)
    }

    @Synchronized
    fun moveQuickSearch(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        val reverse = fromPosition > toPosition
        val offset = if (reverse) toPosition else fromPosition
        val limit = if (reverse) fromPosition - toPosition + 1 else toPosition - fromPosition + 1
        val dao = db.quickSearchDao()
        val list = dao.list(offset, limit)
        val step = if (reverse) 1 else -1
        val start = if (reverse) limit - 1 else 0
        val end = if (reverse) 0 else limit - 1
        val toTime = list[end].time
        var i = end
        while (if (reverse) i < start else i > start) {
            list[i].time = list[i + step].time
            i += step
        }
        list[start].time = toTime
        dao.update(list)
    }

    @get:Synchronized
    val historyLazyList: PagingSource<Int, HistoryInfo>
        get() = db.historyDao().listLazy()

    @Synchronized
    fun putHistoryInfo(galleryInfo: GalleryInfo?) {
        val dao = db.historyDao()
        val info: HistoryInfo = if (galleryInfo is HistoryInfo) {
            galleryInfo
        } else {
            HistoryInfo(galleryInfo!!)
        }
        info.time = System.currentTimeMillis()
        if (null != dao.load(info.gid)) {
            dao.update(info)
        } else {
            dao.insert(info)
        }
    }

    @Synchronized
    fun putHistoryInfoNonRefresh(info: GalleryInfo) {
        val dao = db.historyDao()
        val i = dao.load(info.gid)
        if (null != i) {
            val historyInfo: HistoryInfo
            if (info is HistoryInfo) {
                historyInfo = info
            } else {
                historyInfo = HistoryInfo(info)
                historyInfo.time = i.time
            }
            dao.update(historyInfo)
        }
    }

    @Synchronized
    fun putHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        for (info in historyInfoList) {
            if (null == dao.load(info.gid)) {
                dao.insert(info)
            }
        }
    }

    @Synchronized
    fun deleteHistoryInfo(info: HistoryInfo?) {
        val dao = db.historyDao()
        dao.delete(info!!)
    }

    @Synchronized
    fun clearHistoryInfo() {
        val dao = db.historyDao()
        dao.deleteAll()
    }

    @get:Synchronized
    val allFilter: List<Filter>
        get() = db.filterDao().list()

    @Synchronized
    fun addFilter(filter: Filter): Boolean {
        val existFilter: Filter? = try {
            db.filterDao().load(filter.text!!, filter.mode)
        } catch (e: Exception) {
            null
        }
        return if (existFilter == null) {
            filter.id = null
            filter.id = db.filterDao().insert(filter)
            true
        } else {
            false
        }
    }

    @Synchronized
    fun deleteFilter(filter: Filter) {
        db.filterDao().delete(filter)
    }

    @Synchronized
    fun triggerFilter(filter: Filter) {
        filter.enable = filter.enable?.not() ?: false
        db.filterDao().update(filter)
    }

    private fun <T> copyDao(from: BasicDao<T>, to: BasicDao<T>) {
        val list = from.list()
        for (item in list) to.insert(item)
    }

    @Synchronized
    fun exportDB(context: Context, uri: Uri?): Boolean {
        val ehExportName = "eh.export.db"

        // Delete old export db
        context.deleteDatabase(ehExportName)
        val newDb =
            databaseBuilder(context, EhDatabase::class.java, ehExportName).allowMainThreadQueries()
                .build()
        return try {
            // Copy data to a export db
            copyDao(db.downloadsDao(), newDb.downloadsDao())
            copyDao(db.downloadLabelDao(), newDb.downloadLabelDao())
            copyDao(db.downloadDirnameDao(), newDb.downloadDirnameDao())
            copyDao(db.historyDao(), newDb.historyDao())
            copyDao(db.quickSearchDao(), newDb.quickSearchDao())
            copyDao(db.localFavoritesDao(), newDb.localFavoritesDao())
            copyDao(db.filterDao(), newDb.filterDao())

            // Close export db so we can copy it
            if (newDb.isOpen) {
                newDb.close()
            }

            // Copy export db to data dir
            val dbFile = context.getDatabasePath(ehExportName)
            if (dbFile == null || !dbFile.isFile) {
                return false
            }
            var `is`: InputStream? = null
            var os: OutputStream? = null
            try {
                `is` = FileInputStream(dbFile)
                os = context.contentResolver.openOutputStream(uri!!)
                IOUtils.copy(`is`, os)
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                IOUtils.closeQuietly(`is`)
                IOUtils.closeQuietly(os)
            }

            // Delete failed file
            false
        } finally {
            context.deleteDatabase(ehExportName)
        }
    }

    /**
     * @return error string, null for no error
     */
    @Synchronized
    fun importDB(context: Context, uri: Uri?): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri!!)

            // Copy file to cache dir
            val file = File.createTempFile("importDatabase", "")
            val outputStream = FileOutputStream(file)
            val buff = ByteArray(1024)
            var read: Int
            if (inputStream != null) {
                while (inputStream.read(buff, 0, buff.size).also { read = it } > 0) {
                    outputStream.write(buff, 0, read)
                }
            } else {
                return context.getString(R.string.cant_read_the_file)
            }
            inputStream.close()
            outputStream.close()

            // Check database version
            val oldDB = SQLiteDatabase.openDatabase(
                file.path, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            val newVersion = CUR_DB_VER
            val oldVersion = oldDB.version
            if (oldVersion < newVersion) {
                return context.getString(R.string.db_version_too_low)
            } else if (oldVersion > newVersion) {
                return context.getString(R.string.db_version_too_high)
            }

            // Crete temp room database from cache file
            val tmpDBName = "tmp.db"
            context.deleteDatabase(tmpDBName)
            val oldRoomDatabase = databaseBuilder(context, EhDatabase::class.java, tmpDBName)
                .createFromFile(file).allowMainThreadQueries().build()

            // Download label
            val manager = DownloadManager
            try {
                val downloadLabelList = oldRoomDatabase.downloadLabelDao().list()
                manager.addDownloadLabel(downloadLabelList)
            } catch (ignored: Exception) {
            }

            // Downloads
            try {
                val downloadInfoList = oldRoomDatabase.downloadsDao().list()
                manager.addDownload(downloadInfoList, false)
            } catch (ignored: Exception) {
            }

            // Download dirname
            try {
                val downloadDirnameList = oldRoomDatabase.downloadDirnameDao().list()
                for ((gid, dirname1) in downloadDirnameList) {
                    putDownloadDirname(gid, dirname1)
                }
            } catch (ignored: Exception) {
            }

            // History
            try {
                val historyInfoList = oldRoomDatabase.historyDao().list()
                putHistoryInfo(historyInfoList)
            } catch (ignored: Exception) {
            }

            // QuickSearch
            try {
                val quickSearchList = oldRoomDatabase.quickSearchDao().list()
                val currentQuickSearchList = db.quickSearchDao().list()
                val importList: MutableList<QuickSearch?> = ArrayList()
                for (quickSearch in quickSearchList) {
                    var name = quickSearch.name
                    for ((_, name1) in currentQuickSearchList) {
                        if (ObjectUtils.equal(name1, name)) {
                            // The same name
                            name = null
                            break
                        }
                    }
                    if (null == name) {
                        continue
                    }
                    importList.add(quickSearch)
                }
                importQuickSearch(importList)
            } catch (ignored: Exception) {
            }

            // LocalFavorites
            try {
                val localFavoriteInfoList = oldRoomDatabase.localFavoritesDao().list()
                for (info in localFavoriteInfoList) {
                    putLocalFavorites(info)
                }
            } catch (ignored: Exception) {
            }

            // Filter
            try {
                val filterList = oldRoomDatabase.filterDao().list()
                val currentFilterList = db.filterDao().list()
                for (filter in filterList) {
                    if (!currentFilterList.contains(filter)) {
                        addFilter(filter)
                    }
                }
            } catch (ignored: Exception) {
            }

            // Delete temp database
            if (oldRoomDatabase.isOpen) {
                oldRoomDatabase.close()
            }
            context.deleteDatabase(tmpDBName)
            null
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            // Ignore
            context.getString(R.string.cant_read_the_file)
        }
    }
}