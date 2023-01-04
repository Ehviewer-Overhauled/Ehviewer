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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.parser.FavoritesParser
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.util.ExceptionUtils
import com.hippo.util.IoThreadPoolExecutor
import com.hippo.util.LogCat
import com.hippo.util.ReadableTime
import com.hippo.yorozuya.IOUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.ceil

class AdvancedFragment : BasePreferenceFragment() {
    private var exportLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // grantUriPermission might throw RemoteException on MIUI
                requireActivity().grantUriPermission(
                    BuildConfig.APPLICATION_ID,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                ExceptionUtils.throwIfFatal(e)
                e.printStackTrace()
            }
            try {
                val alertDialog = BaseDialogBuilder(requireActivity())
                    .setCancelable(false)
                    .setView(R.layout.preference_dialog_task)
                    .show()
                IoThreadPoolExecutor.getInstance().execute {
                    val success = EhDB.exportDB(requireActivity(), uri)
                    val activity: Activity? = activity
                    activity?.runOnUiThread {
                        if (alertDialog.isShowing) {
                            alertDialog.dismiss()
                        }
                        showTip(
                            if (success) GetText.getString(
                                R.string.settings_advanced_export_data_to,
                                uri.toString()
                            ) else GetText.getString(R.string.settings_advanced_export_data_failed),
                            BaseScene.LENGTH_SHORT
                        )
                    }
                }
            } catch (e: Exception) {
                showTip(R.string.settings_advanced_export_data_failed, BaseScene.LENGTH_SHORT)
            }
        }
    }
    private var dumpLogcatLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // grantUriPermission might throw RemoteException on MIUI
                requireActivity().grantUriPermission(
                    BuildConfig.APPLICATION_ID,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                ExceptionUtils.throwIfFatal(e)
                e.printStackTrace()
            }
            try {
                val zipFile = File(AppConfig.getExternalTempDir(), "logs.zip")
                if (zipFile.exists()) {
                    zipFile.delete()
                }
                val files = ArrayList<File>()
                AppConfig.getExternalParseErrorDir()?.listFiles()?.let { files.addAll(it) }
                AppConfig.getExternalCrashDir()?.listFiles()?.let { files.addAll(it) }
                var finished = false
                var origin: BufferedInputStream? = null
                var out: ZipOutputStream? = null
                try {
                    val dest = FileOutputStream(zipFile)
                    out = ZipOutputStream(BufferedOutputStream(dest))
                    val bytes = ByteArray(1024 * 64)
                    for (file in files) {
                        if (!file.isFile) {
                            continue
                        }
                        try {
                            val fi = FileInputStream(file)
                            origin = BufferedInputStream(fi, bytes.size)
                            val entry = ZipEntry(file.name)
                            out.putNextEntry(entry)
                            var count: Int
                            while (origin.read(bytes, 0, bytes.size).also { count = it } != -1) {
                                out.write(bytes, 0, count)
                            }
                            origin.close()
                            origin = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val entry =
                        ZipEntry("logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt")
                    out.putNextEntry(entry)
                    LogCat.save(out)
                    out.closeEntry()
                    out.close()
                    IOUtils.copy(
                        FileInputStream(zipFile),
                        requireActivity().contentResolver.openOutputStream(uri)
                    )
                    finished = true
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    origin?.close()
                    out?.close()
                }
                if (!finished) {
                    finished = LogCat.save(requireActivity().contentResolver.openOutputStream(uri))
                }
                showTip(
                    if (finished) getString(
                        R.string.settings_advanced_dump_logcat_to,
                        uri.toString()
                    ) else getString(R.string.settings_advanced_dump_logcat_failed),
                    BaseScene.LENGTH_SHORT
                )
            } catch (e: Exception) {
                showTip(
                    getString(R.string.settings_advanced_dump_logcat_failed),
                    BaseScene.LENGTH_SHORT
                )
            }
        }
    }
    private var importDataLauncher = registerForActivityResult<Array<String>, Uri>(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // grantUriPermission might throw RemoteException on MIUI
                requireActivity().grantUriPermission(
                    BuildConfig.APPLICATION_ID,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                ExceptionUtils.throwIfFatal(e)
                e.printStackTrace()
            }
            try {
                val alertDialog = BaseDialogBuilder(requireActivity())
                    .setCancelable(false)
                    .setView(R.layout.preference_dialog_task)
                    .show()
                IoThreadPoolExecutor.getInstance().execute {
                    val error = EhDB.importDB(requireActivity(), uri)
                    val activity: Activity? = activity
                    activity?.runOnUiThread {
                        if (alertDialog.isShowing) {
                            alertDialog.dismiss()
                        }
                        if (null == error) {
                            showTip(
                                getString(R.string.settings_advanced_import_data_successfully),
                                BaseScene.LENGTH_SHORT
                            )
                        } else {
                            showTip(error, BaseScene.LENGTH_SHORT)
                        }
                    }
                }
            } catch (e: Exception) {
                showTip(e.localizedMessage, BaseScene.LENGTH_SHORT)
            }
        }
    }
    private var favTotal = 0
    private var favIndex = 0
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.advanced_settings)
        val dumpLogcat = findPreference<Preference>(KEY_DUMP_LOGCAT)
        val appLanguage = findPreference<Preference>(KEY_APP_LANGUAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) appLanguage!!.isVisible = false
        val importData = findPreference<Preference>(KEY_IMPORT_DATA)
        val exportData = findPreference<Preference>(KEY_EXPORT_DATA)
        val backupFavorite = findPreference<Preference>(KEY_BACKUP_FAVORITE)
        val openByDefault = findPreference<Preference>(KEY_OPEN_BY_DEFAULT)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            openByDefault!!.isVisible = false
        } else {
            openByDefault!!.onPreferenceClickListener = this
        }
        dumpLogcat!!.onPreferenceClickListener = this
        importData!!.onPreferenceClickListener = this
        exportData!!.onPreferenceClickListener = this
        backupFavorite!!.onPreferenceClickListener = this
        appLanguage!!.onPreferenceChangeListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key
        if (KEY_DUMP_LOGCAT == key) {
            try {
                dumpLogcatLauncher.launch("log-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".zip")
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT)
            }
            return true
        } else if (KEY_IMPORT_DATA == key) {
            try {
                importDataLauncher.launch(arrayOf("*/*"))
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT)
            }
            return true
        } else if (KEY_EXPORT_DATA == key) {
            try {
                exportLauncher.launch(ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".db")
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT)
            }
            return true
        } else if (KEY_BACKUP_FAVORITE == key) {
            try {
                backupFavorite()
            } catch (e: Exception) {
                ExceptionUtils.throwIfFatal(e)
                showTip(R.string.settings_advanced_backup_favorite_failed, BaseScene.LENGTH_SHORT)
            }
            return true
        } else if (KEY_OPEN_BY_DEFAULT == key) {
            try {
                @SuppressLint("InlinedApi") val intent = Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:" + requireContext().packageName)
                )
                startActivity(intent)
            } catch (t: Throwable) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + requireContext().packageName)
                )
                startActivity(intent)
            }
            return true
        }
        return false
    }

    private fun backupFavorite() {
        val mClient = EhClient
        val favListUrlBuilder = FavListUrlBuilder()
        favTotal = 0
        favIndex = 1
        val request = EhRequest()
        request.setMethod(EhClient.METHOD_GET_FAVORITES)
        request.setCallback(object : EhClient.Callback<FavoritesParser.Result> {
            override fun onSuccess(result: FavoritesParser.Result) {
                try {
                    if (result.galleryInfoList.isEmpty()) {
                        showTip(
                            R.string.settings_advanced_backup_favorite_nothing,
                            BaseScene.LENGTH_SHORT
                        )
                    } else {
                        if (favTotal == 0 && result.countArray != null) {
                            var totalFav = 0
                            for (i in 0..9) {
                                totalFav += result.countArray[i]
                            }
                            favTotal =
                                ceil(totalFav.toDouble() / result.galleryInfoList.size).toInt()
                        }
                        val status = "($favIndex/$favTotal)"
                        showTip(
                            GetText.getString(
                                R.string.settings_advanced_backup_favorite_start,
                                status
                            ), BaseScene.LENGTH_SHORT
                        )
                        Log.d("LocalFavorites", "now backup page $status")
                        EhDB.putLocalFavorites(result.galleryInfoList)
                        if (result.next != null) {
                            try {
                                runBlocking {
                                    delay(com.hippo.ehviewer.Settings.getDownloadDelay().toLong())
                                }
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                            favIndex++
                            favListUrlBuilder.setIndex(result.next, true)
                            request.setArgs(favListUrlBuilder.build())
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(100)
                                mClient.execute(
                                    request,
                                    viewLifecycleOwner.lifecycle.coroutineScope
                                )
                            }
                        } else {
                            showTip(
                                R.string.settings_advanced_backup_favorite_success,
                                BaseScene.LENGTH_SHORT
                            )
                        }
                    }
                } catch (e: Exception) {
                    showTip(
                        R.string.settings_advanced_backup_favorite_failed,
                        BaseScene.LENGTH_SHORT
                    )
                }
            }

            override fun onFailure(e: Exception) {
                showTip(R.string.settings_advanced_backup_favorite_failed, BaseScene.LENGTH_SHORT)
            }

            override fun onCancel() {
                showTip(R.string.settings_advanced_backup_favorite_failed, BaseScene.LENGTH_SHORT)
            }
        })
        request.setArgs(favListUrlBuilder.build())
        mClient.execute(request, viewLifecycleOwner.lifecycle.coroutineScope)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val key = preference.key
        if (KEY_APP_LANGUAGE == key && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if ("system" == newValue) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newValue as String))
            }
            requireActivity().recreate()
            return true
        }
        return false
    }

    override val fragmentTitle: Int
        get() = R.string.settings_advanced

    companion object {
        private const val KEY_DUMP_LOGCAT = "dump_logcat"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_IMPORT_DATA = "import_data"
        private const val KEY_EXPORT_DATA = "export_data"
        private const val KEY_OPEN_BY_DEFAULT = "open_by_default"
        private const val KEY_BACKUP_FAVORITE = "backup_favorite"
    }
}