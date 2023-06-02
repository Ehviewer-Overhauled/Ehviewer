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
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.hippo.ehviewer.download.DownloadManager as downloadManager

private val NO_REDUNDANCY =
    GetText.getString(R.string.settings_download_clean_redundancy_no_redundancy)
private val CLEAR_REDUNDANCY_DONE =
    { cnt: Int -> GetText.getString(R.string.settings_download_clean_redundancy_done, cnt) }
private val FINAL_CLEAR_REDUNDANCY_MSG =
    { cnt: Int -> if (cnt == 0) NO_REDUNDANCY else CLEAR_REDUNDANCY_DONE(cnt) }

class CleanRedundancyPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TaskPreference(context, attrs) {
    private fun clearFile(file: UniFile): Boolean {
        var name = file.name ?: return false
        val index = name.indexOf('-')
        if (index >= 0) {
            name = name.substring(0, index)
        }
        val gid = name.toLongOrNull() ?: return false
        if (downloadManager.containDownloadInfo(gid)) {
            return false
        }
        file.delete()
        return true
    }

    private fun doRealWork(): Int {
        return downloadLocation.listFiles()?.sumOf { clearFile(it).compareTo(false) } ?: 0
    }

    override fun launchJob() {
        if (singletonJob?.isActive == true) {
            singletonJob?.invokeOnCompletion {
                launchUI {
                    dialog.dismiss()
                }
            }
        } else {
            singletonJob = launch {
                val cnt = doRealWork()
                withUIContext {
                    showTip(FINAL_CLEAR_REDUNDANCY_MSG(cnt))
                    dialog.dismiss()
                }
            }
        }
    }
}

private var singletonJob: Job? = null
