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
package com.hippo.ehviewer.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchNonCancellable

class DownloadFragment : BasePreferenceFragment() {
    private var mDownloadLocation: Preference? = null
    private var pickImageDirLauncher = registerForActivityResult<Uri?, Uri>(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            requireActivity().contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val uniFile = UniFile.fromTreeUri(activity, treeUri)
            if (uniFile != null) {
                Settings.putDownloadLocation(uniFile)
                lifecycleScope.launchNonCancellable {
                    keepNoMediaFileStatus()
                }
                onUpdateDownloadLocation()
            } else {
                showTip(
                    R.string.settings_download_cant_get_download_location,
                    BaseScene.LENGTH_SHORT,
                )
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.download_settings)
        val mediaScan = findPreference<Preference>(Settings.KEY_MEDIA_SCAN)
        mDownloadLocation = findPreference(KEY_DOWNLOAD_LOCATION)
        onUpdateDownloadLocation()
        mediaScan!!.onPreferenceChangeListener = this
        if (mDownloadLocation != null) {
            mDownloadLocation!!.onPreferenceClickListener = this
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadLocation = null
    }

    private fun onUpdateDownloadLocation() {
        val file = Settings.downloadLocation
        if (mDownloadLocation != null) {
            if (file != null) {
                mDownloadLocation!!.summary = file.uri.toString()
            } else {
                mDownloadLocation!!.setSummary(R.string.settings_download_invalid_download_location)
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key
        if (KEY_DOWNLOAD_LOCATION == key) {
            val file = Settings.downloadLocation
            if (file != null && !UniFile.isFileUri(
                    Settings.downloadLocation!!.uri,
                )
            ) {
                BaseDialogBuilder(requireContext())
                    .setTitle(R.string.settings_download_download_location)
                    .setMessage(file.uri.toString())
                    .setPositiveButton(R.string.pick_new_download_location) { _, _ -> openDirPickerL() }
                    .setNeutralButton(R.string.reset_download_location) { _, _ ->
                        val uniFile = UniFile.fromFile(AppConfig.getDefaultDownloadDir())
                        if (uniFile != null) {
                            Settings.putDownloadLocation(uniFile)
                            lifecycleScope.launchNonCancellable {
                                keepNoMediaFileStatus()
                            }
                            onUpdateDownloadLocation()
                        } else {
                            showTip(
                                R.string.settings_download_cant_get_download_location,
                                BaseScene.LENGTH_SHORT,
                            )
                        }
                    }
                    .show()
            } else {
                openDirPickerL()
            }
            return true
        }
        return false
    }

    private fun openDirPickerL() {
        try {
            pickImageDirLauncher.launch(null)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val key = preference.key
        if (Settings.KEY_MEDIA_SCAN == key) {
            if (newValue is Boolean) {
                lifecycleScope.launchNonCancellable {
                    keepNoMediaFileStatus()
                }
            }
            return true
        }
        return false
    }

    override val fragmentTitle: Int
        get() = R.string.settings_download

    companion object {
        const val KEY_DOWNLOAD_LOCATION = "download_location"
    }
}
