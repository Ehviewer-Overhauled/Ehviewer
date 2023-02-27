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
package com.hippo.ehviewer.preference

import android.content.Context
import android.util.AttributeSet
import com.hippo.ehviewer.download.DownloadManager as downloadManager
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine.fillGalleryListByApi
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.readCompatFromUniFile
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

private val RESTORE_NOT_FOUND = GetText.getString(R.string.settings_download_restore_not_found)
private val RESTORE_FAILED = GetText.getString(R.string.settings_download_restore_failed)
private val RESTORE_COUNT_MSG =
    { cnt: Int -> if (cnt == 0) RESTORE_NOT_FOUND else GetText.getString(R.string.settings_download_restore_successfully, cnt) }

class RestoreDownloadPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TaskPreference(context, attrs) {
    private val mManager: DownloadManager = downloadManager
    private var restoreDirCount = 0
    private fun getRestoreItem(file: UniFile?): RestoreItem? {
        if (null == file || !file.isDirectory) return null
        val siFile = file.findFile(SpiderQueen.SPIDER_INFO_FILENAME) ?: return null
        return runCatching {
            val spiderInfo = readCompatFromUniFile(siFile) ?: return null
            var gid = spiderInfo.gid
            var dirname = file.name
            if (mManager.containDownloadInfo(gid)) {
                // Restore download dir to avoid redownload
                val dbdirname = EhDB.getDownloadDirname(gid)
                if (null == dbdirname || dirname != dbdirname) {
                    EhDB.putDownloadDirname(gid, dirname)
                    restoreDirCount++
                }
                return null
            }
            RestoreItem().apply {
                gid = spiderInfo.gid
                token = spiderInfo.token
                dirname = file.name
            }
        }.getOrNull()
    }

    private suspend fun doRealWork(): List<GalleryInfo>? {
        val dir = Settings.downloadLocation ?: return null
        val files = dir.listFiles() ?: return null
        val restoreItemList = files.mapNotNull { getRestoreItem(it) }
        return runSuspendCatching {
            fillGalleryListByApi(restoreItemList, EhUrl.referer)
        }.getOrNull()
    }

    private class RestoreItem : BaseGalleryInfo() {
        var dirname: String? = null
    }

    override fun launchJob() {
        if (singletonJob?.isActive == true) singletonJob?.invokeOnCompletion {
            launchUI {
                dialog.dismiss()
            }
        }
        else singletonJob = launch {
            val result = doRealWork()
            withUIContext {
                if (result == null) {
                    showTip(RESTORE_FAILED)
                } else {
                    if (result.isEmpty()) {
                        showTip(RESTORE_COUNT_MSG(restoreDirCount))
                    } else {
                        var count = 0
                        var i = 0
                        val n = result.size
                        while (i < n) {
                            val item = result[i]
                            // Avoid failed gallery info
                            if (null != item.title) {
                                // Put to download
                                mManager.addDownload(item, null)
                                // Put download dir to DB
                                EhDB.putDownloadDirname(item.gid, (item as RestoreItem).dirname)
                                count++
                            }
                            i++
                        }
                        showTip(RESTORE_COUNT_MSG(count + restoreDirCount))
                    }
                }
                dialog.dismiss()
            }
        }
    }
}

private var singletonJob: Job? = null
