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

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.hippo.app.ListCheckBoxDialogBuilder
import com.hippo.ehviewer.EhApplication.Companion.application
import com.hippo.ehviewer.EhApplication.Companion.downloadManager
import com.hippo.ehviewer.EhApplication.Companion.favouriteStatusRouter
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.IOUtils
import com.hippo.yorozuya.collect.LongList
import java.io.IOException
import java.io.InputStream

object CommonOperations {
    private fun doAddToFavorites(
        activity: Activity, galleryInfo: GalleryInfo,
        slot: Int, listener: EhClient.Callback<Void?>
    ) {
        when (slot) {
            -1 -> {
                EhDB.putLocalFavorites(galleryInfo)
                listener.onSuccess(null)
            }

            in 0..9 -> {
                val request = EhRequest()
                request.setMethod(EhClient.METHOD_ADD_FAVORITES)
                request.setArgs(galleryInfo.gid, galleryInfo.token, slot, "")
                request.setCallback(listener)
                request.enqueue(activity)
            }

            else -> {
                listener.onFailure(Exception()) // TODO Add text
            }
        }
    }

    @JvmOverloads
    fun addToFavorites(
        activity: Activity, galleryInfo: GalleryInfo,
        listener: EhClient.Callback<Void?>, select: Boolean = false
    ) {
        val slot = Settings.getDefaultFavSlot()
        val localFav = activity.getString(R.string.local_favorites)
        val items = Settings.getFavCat().toMutableList().apply { add(0, localFav) }
        if (!select && slot >= -1 && slot <= 9) {
            val newFavoriteName = if (slot >= 0) items[slot + 1] else null
            doAddToFavorites(
                activity,
                galleryInfo,
                slot,
                DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot)
            )
        } else {
            ListCheckBoxDialogBuilder(
                activity, items,
                { builder: ListCheckBoxDialogBuilder?, _: AlertDialog?, position: Int ->
                    val slot1 = position - 1
                    val newFavoriteName = if (slot1 in 0..9) items[slot1 + 1] else null
                    doAddToFavorites(
                        activity,
                        galleryInfo,
                        slot1,
                        DelegateFavoriteCallback(listener, galleryInfo, newFavoriteName, slot1)
                    )
                    if (builder?.isChecked == true) {
                        Settings.putDefaultFavSlot(slot1)
                    } else {
                        Settings.putDefaultFavSlot(Settings.INVALID_DEFAULT_FAV_SLOT)
                    }
                }, activity.getString(R.string.remember_favorite_collection), false
            )
                .setTitle(R.string.add_favorites_dialog_title)
                .setOnCancelListener { listener.onCancel() }
                .show()
        }
    }

    fun removeFromFavorites(
        activity: Activity?, galleryInfo: GalleryInfo,
        listener: EhClient.Callback<Void?>
    ) {
        EhDB.removeLocalFavorites(galleryInfo.gid)
        val request = EhRequest()
        request.setMethod(EhClient.METHOD_ADD_FAVORITES)
        request.setArgs(galleryInfo.gid, galleryInfo.token, -1, "")
        request.setCallback(DelegateFavoriteCallback(listener, galleryInfo, null, -2))
        request.enqueue(activity!!)
    }

    fun startDownload(activity: MainActivity?, galleryInfo: GalleryInfo, forceDefault: Boolean) {
        startDownload(activity!!, listOf(galleryInfo), forceDefault)
    }

    fun startDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.topActivity?.checkAndRequestNotificationPermission()
        }
        doStartDownload(activity, galleryInfos, forceDefault)
    }

    private fun doStartDownload(
        activity: MainActivity,
        galleryInfos: List<GalleryInfo>,
        forceDefault: Boolean
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
        if (!justStart && Settings.getHasDefaultDownloadLabel()) {
            label = Settings.getDefaultDownloadLabel()
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
                activity, items,
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
                }, activity.getString(R.string.remember_download_label), false
            )
                .setTitle(R.string.download)
                .show()
        }
    }

    @JvmStatic
    fun ensureNoMediaFile(file: UniFile?) {
        if (null == file) {
            return
        }
        val noMedia = file.createFile(".nomedia") ?: return
        var `is`: InputStream? = null
        try {
            `is` = noMedia.openInputStream()
        } catch (e: IOException) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(`is`)
        }
    }

    @JvmStatic
    fun removeNoMediaFile(file: UniFile?) {
        if (null == file) {
            return
        }
        val noMedia = file.subFile(".nomedia")
        if (null != noMedia && noMedia.isFile) {
            noMedia.delete()
        }
    }

    private class DelegateFavoriteCallback(
        private val delegate: EhClient.Callback<Void?>, private val info: GalleryInfo,
        private val newFavoriteName: String?, private val slot: Int
    ) : EhClient.Callback<Void?> {
        override fun onSuccess(result: Void?) {
            info.favoriteName = newFavoriteName
            info.favoriteSlot = slot
            delegate.onSuccess(result)
            favouriteStatusRouter.modifyFavourites(info.gid, slot)
        }

        override fun onFailure(e: Exception) {
            delegate.onFailure(e)
        }

        override fun onCancel() {
            delegate.onCancel()
        }
    }
}