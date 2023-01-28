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

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.hippo.ehviewer.EhApplication.Companion.downloadManager
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine.fillGalleryListByApi
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.unifile.UniFile
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.IOUtils
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.util.Collections

class RestoreDownloadPreference constructor(
    context: Context, attrs: AttributeSet?
) : TaskPreference(context, attrs) {
    override fun onCreateTask(): Task {
        return RestoreTask(context)
    }

    private class RestoreTask(context: Context) : Task(context) {
        private val mManager: DownloadManager = downloadManager
        private var restoreDirCount = 0
        private fun getRestoreItem(file: UniFile?): RestoreItem? {
            if (null == file || !file.isDirectory) {
                return null
            }
            val siFile = file.findFile(SpiderQueen.SPIDER_INFO_FILENAME) ?: return null
            return try {
                val spiderInfo = SpiderInfo.read(siFile) ?: return null
                val gid = spiderInfo.gid
                val dirname = file.name
                if (mManager.containDownloadInfo(gid)) {
                    // Restore download dir to avoid redownload
                    val dbdirname = EhDB.getDownloadDirname(gid)
                    if (null == dbdirname || dirname != dbdirname) {
                        EhDB.putDownloadDirname(gid, dirname)
                        restoreDirCount++
                    }
                    return null
                }
                val token = spiderInfo.token
                val restoreItem = RestoreItem()
                restoreItem.gid = gid
                restoreItem.token = token
                restoreItem.dirname = dirname
                restoreItem
            } catch (e: IOException) {
                null
            }
        }

        override fun doInBackground(vararg params: Void): Any? {
            val dir = Settings.getDownloadLocation() ?: return null
            val restoreItemList: MutableList<RestoreItem> = ArrayList()
            val files = dir.listFiles() ?: return null
            for (file in files) {
                val restoreItem = getRestoreItem(file)
                if (null != restoreItem) {
                    restoreItemList.add(restoreItem)
                }
            }
            return if (0 == restoreItemList.size) {
                Collections.EMPTY_LIST
            } else try {
                runBlocking {
                    fillGalleryListByApi(
                        ArrayList(restoreItemList),
                        EhUrl.referer
                    )
                }
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(o: Any) {
            if (o !is List<*>) {
                mActivity.showTip(R.string.settings_download_restore_failed, BaseScene.LENGTH_SHORT)
            } else {
                val list = o as List<RestoreItem>
                if (list.isEmpty()) {
                    mActivity.showTip(
                        if (restoreDirCount == 0)
                            mActivity.getString(R.string.settings_download_restore_not_found)
                        else
                            mActivity.getString(
                                R.string.settings_download_restore_successfully,
                                restoreDirCount
                            ),
                        BaseScene.LENGTH_SHORT
                    )
                } else {
                    var count = 0
                    var i = 0
                    val n = list.size
                    while (i < n) {
                        val item = list[i]
                        // Avoid failed gallery info
                        if (null != item.title) {
                            // Put to download
                            mManager.addDownload(item, null)
                            // Put download dir to DB
                            EhDB.putDownloadDirname(item.gid, item.dirname)
                            count++
                        }
                        i++
                    }
                    showTip(
                        mActivity.getString(
                            R.string.settings_download_restore_successfully,
                            count + restoreDirCount
                        ),
                        BaseScene.LENGTH_SHORT
                    )
                    val preference: Preference? = preference
                    if (null != preference) {
                        val context = preference.context
                        if (context is Activity) {
                            context.setResult(Activity.RESULT_OK)
                        }
                    }
                }
            }
            super.onPostExecute(o)
        }
    }

    private class RestoreItem : BaseGalleryInfo() {
        var dirname: String? = null
    }
}