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
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhApplication.Companion.favouriteStatusRouter
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.widget.ListCheckBoxDialogBuilder
import com.hippo.unifile.UniFile
import com.hippo.util.requestPermission
import com.hippo.yorozuya.collect.LongList
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchNow
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import com.hippo.ehviewer.download.DownloadManager as downloadManager

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
        val dm = downloadManager
        val toStart = LongList()
        val toAdd: MutableList<GalleryInfo> = ArrayList()
        for (gi in galleryInfos) {
            if (dm.containDownloadInfo(gi.gid)) {
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
            justStart = label == null || dm.containLabel(label)
        }
        // If there is no other label, just use null label
        if (!justStart && dm.labelList.isEmpty()) {
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
            val list = dm.labelList
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
                        if (!dm.containLabel(label1)) {
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
                        Settings.putHasDefaultDownloadLabel(true)
                        Settings.putDefaultDownloadLabel(label1)
                    } else {
                        Settings.putHasDefaultDownloadLabel(false)
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
        val downloadLocation = Settings.downloadLocation ?: return
        if (Settings.mediaScan) {
            removeNoMediaFile(downloadLocation)
        } else {
            ensureNoMediaFile(downloadLocation)
        }
    }
}

suspend fun Context.addToFavorites(galleryInfo: GalleryInfo, select: Boolean = false) {
    val slot = Settings.defaultFavSlot
    val localFav = getString(R.string.local_favorites)
    val items = Settings.favCat.toMutableList().apply { add(0, localFav) }
    suspend fun doAddToFavorites(slot: Int, newFavoriteName: String?) {
        when (slot) {
            -1 -> {
                EhDB.putLocalFavorites(galleryInfo)
            }

            in 0..9 -> {
                EhEngine.addFavorites(galleryInfo.gid, galleryInfo.token, slot, "")
            }

            else -> {
                throw EhException("Invalid favslot!")
            }
        }
        galleryInfo.favoriteName = newFavoriteName
        galleryInfo.favoriteSlot = slot
        withUIContext {
            favouriteStatusRouter.modifyFavourites(galleryInfo.gid, slot)
        }
    }
    if (!select && slot >= -1 && slot <= 9) {
        val newFavoriteName = if (slot >= 0) items[slot + 1] else null
        doAddToFavorites(slot, newFavoriteName)
    } else {
        val (slot1, checked) = withUIContext {
            suspendCancellableCoroutine { cont ->
                ListCheckBoxDialogBuilder(
                    this@addToFavorites,
                    items,
                    { b, _, position -> cont.resume(position - 1 to b.isChecked) },
                    getString(R.string.remember_favorite_collection),
                    false,
                ).setTitle(R.string.add_favorites_dialog_title).setOnCancelListener { cont.cancel() }.show()
            }
        }
        val newFavoriteName = if (slot1 in 0..9) items[slot1 + 1] else null
        doAddToFavorites(slot1, newFavoriteName)
        if (checked) {
            Settings.putDefaultFavSlot(slot1)
        } else {
            Settings.putDefaultFavSlot(Settings.INVALID_DEFAULT_FAV_SLOT)
        }
    }
}

suspend fun removeFromFavorites(galleryInfo: GalleryInfo) {
    EhDB.removeLocalFavorites(galleryInfo.gid)
    EhEngine.addFavorites(galleryInfo.gid, galleryInfo.token, -1, "")
    galleryInfo.favoriteName = null
    galleryInfo.favoriteSlot = -2
    withUIContext {
        favouriteStatusRouter.modifyFavourites(galleryInfo.gid, -2)
    }
}

fun Context.navToReader(info: GalleryInfo, page: Int) {
    val intent = Intent(this, ReaderActivity::class.java)
    intent.action = ReaderActivity.ACTION_EH
    intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, info)
    intent.putExtra(ReaderActivity.KEY_PAGE, page)
    startActivity(intent)
}
