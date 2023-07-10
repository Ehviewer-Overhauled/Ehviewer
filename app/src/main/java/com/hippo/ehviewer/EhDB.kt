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
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import androidx.paging.PagingSource
import arrow.fx.coroutines.release
import arrow.fx.coroutines.resource
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
import com.hippo.ehviewer.util.sendTo
import splitties.arch.room.roomDb

object EhDB {
    private val db = ehDatabase

    // Fix state
    @get:Synchronized
    val allDownloadInfo: List<DownloadInfo>
        get() = db.downloadsDao().list().onEach {
            if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
                it.state = DownloadInfo.STATE_NONE
            }
        }

    @Synchronized
    fun updateDownloadInfo(downloadInfos: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfos)
    }

    @Synchronized
    fun putDownloadInfo(downloadInfo: DownloadInfo) {
        db.downloadsDao().run {
            if (load(downloadInfo.gid) != null) {
                update(downloadInfo)
            } else {
                insert(downloadInfo)
            }
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

    @Synchronized
    fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        var raw = dao.load(gid)
        if (raw != null) {
            dao.update(raw.copy(dirname = dirname))
        } else {
            raw = DownloadDirname(gid, dirname)
            dao.insert(raw)
        }
    }

    @Synchronized
    fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    @get:Synchronized
    val allDownloadLabelList: List<DownloadLabel>
        get() = db.downloadLabelDao().list()

    @Synchronized
    fun addDownloadLabel(label: String): DownloadLabel {
        val dao = db.downloadLabelDao()
        val raw = DownloadLabel(label, System.currentTimeMillis())
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

    @Synchronized
    fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
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
        if (quickSearch != null) {
            dao.delete(quickSearch)
        }
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

    val historyLazyList: PagingSource<Int, HistoryInfo>
        get() = db.historyDao().listLazy()

    val localFavLazyList: PagingSource<Int, LocalFavoriteInfo>
        get() = db.localFavoritesDao().listLazy()

    fun searchLocalFav(keyword: String) = db.localFavoritesDao().listLazy("%$keyword%")

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
    fun modifyHistoryInfoFavslotNonRefresh(gid: Long, slot: Int) {
        val dao = db.historyDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
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
            db.filterDao().load(filter.text, filter.mode.field)
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
        filter.enable = !filter.enable
        db.filterDao().update(filter)
    }

    private fun <T> copyDao(from: BasicDao<T>, to: BasicDao<T>) {
        val list = from.list()
        for (item in list) to.insert(item)
    }

    suspend fun exportDB(context: Context, uri: Uri) {
        val ehExportName = "eh.export.db"
        resource {
            context.deleteDatabase(ehExportName)
            roomDb<EhDatabase>(ehExportName)
        } release {
            it.close()
            context.deleteDatabase(ehExportName)
        } use { newDb ->
            copyDao(db.downloadsDao(), newDb.downloadsDao())
            copyDao(db.downloadLabelDao(), newDb.downloadLabelDao())
            copyDao(db.downloadDirnameDao(), newDb.downloadDirnameDao())
            copyDao(db.historyDao(), newDb.historyDao())
            copyDao(db.quickSearchDao(), newDb.quickSearchDao())
            copyDao(db.localFavoritesDao(), newDb.localFavoritesDao())
            copyDao(db.filterDao(), newDb.filterDao())
            newDb.close()
            val dbFile = context.getDatabasePath(ehExportName)
            context.contentResolver.openFileDescriptor(uri, "rw")!!.use { toFd ->
                ParcelFileDescriptor.open(dbFile, MODE_READ_ONLY).use { fromFd ->
                    fromFd sendTo toFd
                }
            }
        }
    }

    suspend fun importDB(context: Context, uri: Uri) {
        val tempDBName = "tmp.db"
        resource {
            context.deleteDatabase(tempDBName)
            roomDb<EhDatabase>(tempDBName) { createFromInputStream { context.contentResolver.openInputStream(uri) } }
        } release {
            it.close()
            context.deleteDatabase(tempDBName)
        } use { oldDB ->
            // Download label
            runCatching {
                val downloadLabelList = oldDB.downloadLabelDao().list()
                DownloadManager.addDownloadLabel(downloadLabelList)
            }

            // Downloads
            runCatching {
                val downloadInfoList = oldDB.downloadsDao().list()
                DownloadManager.addDownload(downloadInfoList, false)
            }

            // Download dirname
            runCatching {
                oldDB.downloadDirnameDao().list().forEach {
                    putDownloadDirname(it.gid, it.dirname)
                }
            }

            // History
            runCatching {
                val historyInfoList = oldDB.historyDao().list()
                putHistoryInfo(historyInfoList)
            }

            // QuickSearch
            runCatching {
                val quickSearchList = oldDB.quickSearchDao().list()
                val currentQuickSearchList = db.quickSearchDao().list()
                val importList = quickSearchList.filter { newQS ->
                    currentQuickSearchList.none { it.name == newQS.name }
                }
                importQuickSearch(importList)
            }

            // LocalFavorites
            runCatching {
                oldDB.localFavoritesDao().list().forEach {
                    putLocalFavorites(it)
                }
            }

            // Filter
            runCatching {
                val filterList = oldDB.filterDao().list()
                val currentFilterList = db.filterDao().list()
                filterList.forEach {
                    if (it !in currentFilterList) addFilter(it)
                }
            }
        }
    }
}
