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
import com.hippo.ehviewer.client.data.GalleryInfo
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
import kotlinx.coroutines.flow.Flow
import splitties.arch.room.roomDb

object EhDB {
    private val db = roomDb<EhDatabase>("eh.db")

    suspend fun getAllDownloadInfo() = db.downloadsDao().list().onEach {
        if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
            it.state = DownloadInfo.STATE_NONE
        }
    }

    suspend fun updateDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfo)
    }

    suspend fun putDownloadInfo(downloadInfo: DownloadInfo) {
        db.downloadsDao().run {
            if (load(downloadInfo.gid) != null) {
                update(downloadInfo)
            } else {
                insert(downloadInfo)
            }
        }
    }

    suspend fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        val dao = db.downloadsDao()
        dao.delete(downloadInfo)
        dao.fill(downloadInfo.position)
    }

    suspend fun removeDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.delete(downloadInfo)
        dao.fill(downloadInfo.first().position, downloadInfo.size)
    }

    suspend fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    suspend fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        var raw = dao.load(gid)
        if (raw != null) {
            dao.update(raw.copy(dirname = dirname))
        } else {
            raw = DownloadDirname(gid, dirname)
            dao.insert(raw)
        }
    }

    suspend fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    suspend fun getAllDownloadLabelList() = db.downloadLabelDao().list()

    suspend fun addDownloadLabel(raw: DownloadLabel): DownloadLabel {
        // Reset id
        raw.id = null
        val dao = db.downloadLabelDao()
        raw.id = dao.insert(raw)
        return raw
    }

    suspend fun updateDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.update(raw)
    }

    suspend fun updateDownloadLabel(downloadLabels: List<DownloadLabel>) {
        val dao = db.downloadLabelDao()
        dao.update(downloadLabels)
    }

    suspend fun removeDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.delete(raw)
        dao.fill(raw.position)
    }

    suspend fun removeLocalFavorites(gid: Long) {
        db.localFavoritesDao().deleteByKey(gid)
    }

    suspend fun removeLocalFavorites(gidArray: LongArray) {
        val dao = db.localFavoritesDao()
        for (gid in gidArray) {
            dao.deleteByKey(gid)
        }
    }

    suspend fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
    }

    suspend fun putLocalFavorites(galleryInfo: GalleryInfo) {
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

    suspend fun putLocalFavorites(galleryInfoList: List<GalleryInfo>) {
        for (gi in galleryInfoList) {
            putLocalFavorites(gi)
        }
    }

    suspend fun getAllQuickSearch() = db.quickSearchDao().list()

    suspend fun insertQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        quickSearch.id = dao.insert(quickSearch)
    }

    private suspend fun importQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.insert(quickSearchList)
    }

    suspend fun deleteQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        dao.delete(quickSearch)
        dao.fill(quickSearch.position)
    }

    suspend fun updateQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.update(quickSearchList)
    }

    val historyLazyList: PagingSource<Int, HistoryInfo>
        get() = db.historyDao().listLazy()

    val localFavLazyList: PagingSource<Int, LocalFavoriteInfo>
        get() = db.localFavoritesDao().listLazy()

    val localFavCount: Flow<Int>
        get() = db.localFavoritesDao().count()

    fun searchLocalFav(keyword: String) = db.localFavoritesDao().listLazy("%$keyword%")

    suspend fun putHistoryInfo(galleryInfo: GalleryInfo) {
        val dao = db.historyDao()
        val info: HistoryInfo = if (galleryInfo is HistoryInfo) {
            galleryInfo
        } else {
            HistoryInfo(galleryInfo)
        }
        info.time = System.currentTimeMillis()
        if (null != dao.load(info.gid)) {
            dao.update(info)
        } else {
            dao.insert(info)
        }
    }

    suspend fun modifyHistoryInfoFavslotNonRefresh(gid: Long, slot: Int) {
        val dao = db.historyDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
        }
    }

    private suspend fun putHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        for (info in historyInfoList) {
            if (null == dao.load(info.gid)) {
                dao.insert(info)
            }
        }
    }

    suspend fun deleteHistoryInfo(info: HistoryInfo) {
        val dao = db.historyDao()
        dao.delete(info)
    }

    suspend fun clearHistoryInfo() {
        val dao = db.historyDao()
        dao.deleteAll()
    }

    suspend fun getAllFilter() = db.filterDao().list()

    suspend fun addFilter(filter: Filter): Boolean {
        val existFilter = runCatching { db.filterDao().load(filter.text, filter.mode.field) }.getOrNull()
        return if (existFilter == null) {
            filter.id = null
            filter.id = db.filterDao().insert(filter)
            true
        } else {
            false
        }
    }

    suspend fun deleteFilter(filter: Filter) {
        db.filterDao().delete(filter)
    }

    suspend fun updateFilter(filter: Filter) {
        db.filterDao().update(filter)
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
            db.downloadsDao().list().forEach { newDb.downloadsDao().insert(it) }
            db.downloadLabelDao().list().forEach { newDb.downloadLabelDao().insert(it) }
            db.downloadDirnameDao().list().forEach { newDb.downloadDirnameDao().insert(it) }
            db.historyDao().list().forEach { newDb.historyDao().insert(it) }
            db.quickSearchDao().list().forEach { newDb.quickSearchDao().insert(it) }
            db.localFavoritesDao().list().forEach { newDb.localFavoritesDao().insert(it) }
            db.filterDao().list().forEach { newDb.filterDao().insert(it) }
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
            runCatching {
                val downloadLabelList = oldDB.downloadLabelDao().list()
                DownloadManager.addDownloadLabel(downloadLabelList)
            }
            runCatching {
                val downloadInfoList = oldDB.downloadsDao().list()
                DownloadManager.addDownload(downloadInfoList, false)
            }
            runCatching {
                oldDB.downloadDirnameDao().list().forEach {
                    putDownloadDirname(it.gid, it.dirname)
                }
            }
            runCatching {
                val historyInfoList = oldDB.historyDao().list()
                putHistoryInfo(historyInfoList)
            }
            runCatching {
                val quickSearchList = oldDB.quickSearchDao().list()
                val currentQuickSearchList = db.quickSearchDao().list()
                val offset = currentQuickSearchList.size
                val importList = quickSearchList.filter { newQS ->
                    currentQuickSearchList.none { it.name == newQS.name }
                }.onEachIndexed { index, q -> q.position = index + offset }
                importQuickSearch(importList)
            }
            runCatching {
                oldDB.localFavoritesDao().list().forEach {
                    putLocalFavorites(it)
                }
            }
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
