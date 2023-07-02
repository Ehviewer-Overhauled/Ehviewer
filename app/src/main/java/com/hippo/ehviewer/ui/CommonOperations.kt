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
package com.hippo.ehviewer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ui.legacy.ListCheckBoxDialogBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.requestPermission
import com.hippo.ehviewer.yorozuya.collect.LongList
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchNow
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.util.ContextUtils.requireActivity
import splitties.init.appCtx

object CommonOperations {
    fun startDownload(activity: MainActivity?, galleryInfo: GalleryInfo, forceDefault: Boolean) {
        startDownload(activity!!, listOf(galleryInfo), forceDefault)
    }

    fun startDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean,
    ) {
        launchNow {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
            }
            doStartDownload(activity, galleryInfos, forceDefault)
        }
    }

    private fun doStartDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean,
    ) {
        val toStart = LongList()
        val toAdd: MutableList<GalleryInfo> = ArrayList()
        for (gi in galleryInfos) {
            if (DownloadManager.containDownloadInfo(gi.gid)) {
                toStart.add(gi.gid)
            } else {
                toAdd.add(gi)
            }
        }
        if (!toStart.isEmpty()) {
            val intent = Intent(activity, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START_RANGE
            intent.putExtra(DownloadService.KEY_GID_LIST, toStart)
            ContextCompat.startForegroundService(activity, intent)
        }
        if (toAdd.isEmpty()) {
            activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
            return
        }
        var justStart = forceDefault
        var label: String? = null
        // Get default download label
        if (!justStart && Settings.hasDefaultDownloadLabel) {
            label = Settings.defaultDownloadLabel
            justStart = label == null || DownloadManager.containLabel(label)
        }
        // If there is no other label, just use null label
        if (!justStart && DownloadManager.labelList.isEmpty()) {
            justStart = true
            label = null
        }
        if (justStart) {
            // Got default label
            for (gi in toAdd) {
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START
                intent.putExtra(DownloadService.KEY_LABEL, label)
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
                ContextCompat.startForegroundService(activity, intent)
            }
            // Notify
            activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
        } else {
            // Let use chose label
            val list = DownloadManager.labelList
            val items = mutableListOf<String>()
            items.add(activity.getString(R.string.default_download_label_name))
            items.addAll(list.mapNotNull { it.label })
            ListCheckBoxDialogBuilder(
                activity,
                items,
                { builder: ListCheckBoxDialogBuilder?, _: AlertDialog?, position: Int ->
                    var label1: String?
                    if (position == 0) {
                        label1 = null
                    } else {
                        label1 = items[position]
                        if (!DownloadManager.containLabel(label1)) {
                            label1 = null
                        }
                    }
                    // Start download
                    for (gi in toAdd) {
                        val intent = Intent(activity, DownloadService::class.java)
                        intent.action = DownloadService.ACTION_START
                        intent.putExtra(DownloadService.KEY_LABEL, label1)
                        intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
                        ContextCompat.startForegroundService(activity, intent)
                    }
                    // Save settings
                    if (builder?.isChecked == true) {
                        Settings.hasDefaultDownloadLabel = true
                        Settings.defaultDownloadLabel = label1
                    } else {
                        Settings.hasDefaultDownloadLabel = false
                    }
                    // Notify
                    activity.showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
                },
                activity.getString(R.string.remember_download_label),
                false,
            )
                .setTitle(R.string.download)
                .show()
        }
    }
}

private fun removeNoMediaFile(downloadDir: UniFile) {
    val noMedia = downloadDir.subFile(".nomedia") ?: return
    noMedia.delete()
}

private fun ensureNoMediaFile(downloadDir: UniFile) {
    downloadDir.createFile(".nomedia") ?: return
}

private val lck = Mutex()

suspend fun keepNoMediaFileStatus() {
    lck.withLock {
        if (Settings.mediaScan) {
            removeNoMediaFile(downloadLocation)
        } else {
            ensureNoMediaFile(downloadLocation)
        }
    }
}

suspend fun DialogState.addToFavorites(galleryInfo: GalleryInfo): Boolean {
    val localFaved = EhDB.containLocalFavorites(galleryInfo.gid)
    val localFav = if (localFaved) {
        Icons.Default.Favorite
    } else {
        Icons.Default.FavoriteBorder
    } to appCtx.getString(R.string.local_favorites)
    val items = Settings.favCat.mapIndexed { index, name ->
        if (galleryInfo.favoriteSlot == index) {
            Icons.Default.Favorite
        } else {
            Icons.Default.FavoriteBorder
        } to name
    }.toTypedArray()
    val slot = showSelectItemWithIcon(localFav, *items, title = R.string.add_favorites_dialog_title) - 1
    val newFavoriteName = Settings.favCat.getOrNull(slot)
    return doAddToFavorites(galleryInfo, slot, newFavoriteName)
}

private suspend fun doAddToFavorites(
    galleryInfo: GalleryInfo,
    slot: Int = -2,
    newFavoriteName: String? = null,
    note: String? = null,
    localFaved: Boolean = false,
): Boolean {
    val add = when (slot) {
        -2 -> {
            EhDB.removeLocalFavorites(galleryInfo.gid)
            if (galleryInfo.favoriteSlot >= 0) {
                EhEngine.addFavorites(galleryInfo.gid, galleryInfo.token)
            }
            false
        }

        -1 -> {
            if (localFaved) {
                EhDB.removeLocalFavorites(galleryInfo.gid)
            } else {
                EhDB.putLocalFavorites(galleryInfo)
            }
            !localFaved
        }

        in 0..9 -> {
            if (galleryInfo.favoriteSlot == slot) {
                EhEngine.addFavorites(galleryInfo.gid, galleryInfo.token)
                false
            } else {
                EhEngine.addFavorites(galleryInfo.gid, galleryInfo.token, slot, note)
                true
            }
        }

        else -> throw EhException("Invalid favorite slot!")
    }
    if (add) {
        if (slot != -1 || galleryInfo.favoriteSlot == -2) { // Cloud favorites have priority
            galleryInfo.favoriteSlot = slot
            galleryInfo.favoriteName = newFavoriteName
            withUIContext {
                FavouriteStatusRouter.modifyFavourites(galleryInfo.gid, slot)
            }
        }
    } else if (slot != -1 || galleryInfo.favoriteSlot == -1) {
        val newSlot = if (slot >= 0 && localFaved) -1 else -2
        galleryInfo.favoriteSlot = newSlot
        galleryInfo.favoriteName = null
        withUIContext {
            FavouriteStatusRouter.modifyFavourites(galleryInfo.gid, newSlot)
        }
    }
    return add
}

suspend fun removeFromFavorites(galleryInfo: GalleryInfo) = doAddToFavorites(galleryInfo)

fun Context.navToReader(info: GalleryInfo, page: Int = -1) {
    val intent = Intent(this, ReaderActivity::class.java)
    intent.action = ReaderActivity.ACTION_EH
    intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, info)
    intent.putExtra(ReaderActivity.KEY_PAGE, page)
    startActivity(intent)
}

suspend fun DialogState.doGalleryInfoAction(info: GalleryInfo, context: Context) {
    val downloaded = DownloadManager.getDownloadState(info.gid) != DownloadInfo.STATE_INVALID
    val favourite = info.favoriteSlot != -2
    val selected = if (!downloaded) {
        showSelectItemWithIcon(
            Icons.Default.MenuBook to R.string.read,
            Icons.Default.Download to R.string.download,
            if (!favourite) Icons.Default.Favorite to R.string.add_to_favourites else Icons.Default.HeartBroken to R.string.remove_from_favourites,
            title = EhUtils.getSuitableTitle(info),
        )
    } else {
        showSelectItemWithIcon(
            Icons.Default.MenuBook to R.string.read,
            Icons.Default.Delete to R.string.delete_downloads,
            if (!favourite) Icons.Default.Favorite to R.string.add_to_favourites else Icons.Default.HeartBroken to R.string.remove_from_favourites,
            Icons.Default.DriveFileMove to R.string.download_move_dialog_title,
            title = EhUtils.getSuitableTitle(info),
        )
    }
    with(requireActivity<MainActivity>(context)) {
        when (selected) {
            0 -> navToReader(info)
            1 -> withUIContext {
                if (downloaded) {
                    if (confirmRemoveDownload(info)) DownloadManager.deleteDownload(info.gid)
                } else {
                    CommonOperations.startDownload(this@with, info, false)
                }
            }

            2 -> if (favourite) {
                runSuspendCatching {
                    removeFromFavorites(info)
                    showTip(R.string.remove_from_favorite_success, BaseScene.LENGTH_SHORT)
                }.onFailure {
                    showTip(R.string.remove_from_favorite_failure, BaseScene.LENGTH_LONG)
                }
            } else {
                runSuspendCatching {
                    addToFavorites(info)
                    showTip(R.string.add_to_favorite_success, BaseScene.LENGTH_SHORT)
                }.onFailure {
                    showTip(R.string.add_to_favorite_failure, BaseScene.LENGTH_LONG)
                }
            }

            3 -> showMoveDownloadLabel(info)
        }
        true
    }
}

suspend fun DialogState.confirmRemoveDownload(info: GalleryInfo): Boolean = show(
    confirmText = android.R.string.ok,
    dismissText = android.R.string.cancel,
    title = R.string.download_remove_dialog_title,
    text = { Text(text = stringResource(id = R.string.download_remove_dialog_message, info.title.orEmpty())) },
)

suspend fun DialogState.showMoveDownloadLabel(info: GalleryInfo) {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = DownloadManager.labelList.mapNotNull { it.label }.toTypedArray()
    val selected = showSelectItem(defaultLabel, *labels, title = R.string.download_move_dialog_title)
    val downloadInfo = DownloadManager.getDownloadInfo(info.gid) ?: return
    val label = if (selected == 0) null else labels[selected - 1]
    withUIContext { DownloadManager.changeLabel(listOf(downloadInfo), label) }
}
