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
import arrow.fx.coroutines.release
import arrow.fx.coroutines.resource
import com.hippo.ehviewer.client.data.BaseGalleryInfo
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

    private suspend fun putGalleryInfo(galleryInfo: BaseGalleryInfo) {
        db.galleryDao().upsert(galleryInfo)
    }

    private suspend fun deleteGalleryInfo(galleryInfo: BaseGalleryInfo) {
        runCatching { db.galleryDao().delete(galleryInfo) }
    }

    suspend fun getAllDownloadInfo() = db.downloadsDao().joinList().onEach {
        if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
            it.state = DownloadInfo.STATE_NONE
        }
    }

    suspend fun updateDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfo.map(DownloadInfo::downloadInfo))
    }

    suspend fun putDownloadInfo(downloadInfo: DownloadInfo) {
        putGalleryInfo(downloadInfo.galleryInfo)
        db.downloadsDao().upsert(downloadInfo.downloadInfo)
    }

    suspend fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        val dao = db.downloadsDao()
        dao.delete(downloadInfo.downloadInfo)
        dao.fill(downloadInfo.position)
        deleteGalleryInfo(downloadInfo.galleryInfo)
    }

    suspend fun removeDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        downloadInfo.forEach {
            dao.delete(it.downloadInfo)
            deleteGalleryInfo(it.galleryInfo)
        }
        dao.fill(downloadInfo.first().position, downloadInfo.size)
    }

    suspend fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    suspend fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        dao.upsert(DownloadDirname(gid, dirname))
    }

    suspend fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    private suspend fun importDownloadDirname(downloadDirnameList: List<DownloadDirname>) {
        val dao = db.downloadDirnameDao()
        dao.insertOrIgnore(downloadDirnameList)
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

    suspend fun removeLocalFavorites(galleryInfo: BaseGalleryInfo) {
        db.localFavoritesDao().deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo)
    }

    suspend fun removeLocalFavorites(galleryInfoList: List<BaseGalleryInfo>) {
        galleryInfoList.forEach {
            removeLocalFavorites(it)
        }
    }

    suspend fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
    }

    suspend fun putLocalFavorites(galleryInfo: BaseGalleryInfo) {
        putGalleryInfo(galleryInfo)
        db.localFavoritesDao().upsert(LocalFavoriteInfo(galleryInfo.gid))
    }

    suspend fun putLocalFavorites(galleryInfoList: List<BaseGalleryInfo>) {
        galleryInfoList.forEach {
            putLocalFavorites(it)
        }
    }

    private suspend fun importLocalFavorites(localFavorites: List<LocalFavoriteInfo>) {
        db.localFavoritesDao().insertOrIgnore(localFavorites)
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

    val historyLazyList
        get() = db.historyDao().joinListLazy()

    val localFavLazyList
        get() = db.localFavoritesDao().joinListLazy()

    val localFavCount: Flow<Int>
        get() = db.localFavoritesDao().count()

    fun searchLocalFav(keyword: String) = db.localFavoritesDao().joinListLazy("%$keyword%")

    suspend fun putHistoryInfo(galleryInfo: BaseGalleryInfo) {
        putGalleryInfo(galleryInfo)
        db.historyDao().upsert(HistoryInfo(galleryInfo.gid))
    }

    suspend fun modifyHistoryInfoFavslotNonRefresh(gid: Long, slot: Int) {
        val dao = db.galleryDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
        }
    }

    private suspend fun importHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        dao.insertOrIgnore(historyInfoList)
    }

    suspend fun deleteHistoryInfo(galleryInfo: BaseGalleryInfo) {
        val dao = db.historyDao()
        dao.deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo)
    }

    suspend fun clearHistoryInfo() {
        val dao = db.historyDao()
        val historyList = dao.list()
        dao.deleteAll()
        historyList.forEach { runCatching { db.galleryDao().deleteByKey(it.gid) } }
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
            db.galleryDao().list().let { newDb.galleryDao().insertOrIgnore(it) }
            db.downloadLabelDao().list().let { newDb.downloadLabelDao().insert(it) }
            db.downloadDirnameDao().list().let { newDb.downloadDirnameDao().insertOrIgnore(it) }
            db.downloadsDao().list().let { newDb.downloadsDao().insert(it) }
            db.historyDao().list().let { newDb.historyDao().insertOrIgnore(it) }
            db.quickSearchDao().list().let { newDb.quickSearchDao().insert(it) }
            db.localFavoritesDao().list().let { newDb.localFavoritesDao().insertOrIgnore(it) }
            db.filterDao().list().let { newDb.filterDao().insert(it) }
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
                db.galleryDao().insertOrIgnore(oldDB.galleryDao().list())
            }
            runCatching {
                val downloadLabelList = oldDB.downloadLabelDao().list()
                DownloadManager.addDownloadLabel(downloadLabelList)
            }
            runCatching {
                oldDB.downloadDirnameDao().list().let {
                    importDownloadDirname(it)
                }
            }
            runCatching {
                val downloadInfoList = oldDB.downloadsDao().joinList().asReversed()
                DownloadManager.addDownload(downloadInfoList, false)
            }
            runCatching {
                val historyInfoList = oldDB.historyDao().list()
                importHistoryInfo(historyInfoList)
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
                oldDB.localFavoritesDao().list().let {
                    importLocalFavorites(it)
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
