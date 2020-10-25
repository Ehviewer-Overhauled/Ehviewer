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

package com.hippo.ehviewer.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.SettingsActivity;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.util.LogCat;
import com.hippo.util.ReadableTime;

public class AdvancedFragment extends BaseSettingsFragment {

    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    private static final int REQUEST_DUMP_LOGCAT = 3;

    private static final String KEY_DUMP_LOGCAT = "dump_logcat";
    private static final String KEY_CLEAR_MEMORY_CACHE = "clear_memory_cache";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_IMPORT_DATA = "import_data";
    private static final String KEY_EXPORT_DATA = "export_data";

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.advanced_settings);

        Preference dumpLogcat = findPreference(KEY_DUMP_LOGCAT);
        Preference clearMemoryCache = findPreference(KEY_CLEAR_MEMORY_CACHE);
        Preference appLanguage = findPreference(KEY_APP_LANGUAGE);
        Preference importData = findPreference(KEY_IMPORT_DATA);
        Preference exportData = findPreference(KEY_EXPORT_DATA);

        dumpLogcat.setOnPreferenceClickListener(this);
        clearMemoryCache.setOnPreferenceClickListener(this);
        importData.setOnPreferenceClickListener(this);
        exportData.setOnPreferenceClickListener(this);

        appLanguage.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DUMP_LOGCAT.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_TITLE, "logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt");
            startActivityForResult(intent, REQUEST_DUMP_LOGCAT);
            return true;
        } else if (KEY_CLEAR_MEMORY_CACHE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).clearMemoryCache();
            Runtime.getRuntime().gc();
        } else if (KEY_IMPORT_DATA.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
            return true;
        } else if (KEY_EXPORT_DATA.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_TITLE, ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".db");
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(PreferenceGroup group, int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_EXPORT) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
                                .setCancelable(false)
                                .setView(R.layout.preference_dialog_task)
                                .show();
                        new Thread(() -> {
                            boolean success = EhDB.exportDB(requireActivity(), uri);
                            requireActivity().runOnUiThread(() -> {
                                if (alertDialog.isShowing()) {
                                    alertDialog.dismiss();
                                }
                                ((SettingsActivity) requireActivity()).showTip(
                                        (success)
                                                ? GetText.getString(R.string.settings_advanced_export_data_to, uri.toString())
                                                : GetText.getString(R.string.settings_advanced_export_data_failed),
                                        BaseScene.LENGTH_SHORT);
                            });
                        }).start();
                    } catch (Exception e) {
                        ((SettingsActivity) requireActivity()).showTip(R.string.settings_advanced_export_data_failed, BaseScene.LENGTH_SHORT);
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireActivity())
                                .setCancelable(false)
                                .setView(R.layout.preference_dialog_task)
                                .show();
                        new Thread(() -> {
                            final String error = EhDB.importDB(requireActivity(), uri);
                            requireActivity().runOnUiThread(() -> {
                                if (alertDialog.isShowing()) {
                                    alertDialog.dismiss();
                                }
                                if (null == error) {
                                    ((SettingsActivity) requireActivity()).showTip(getString(R.string.settings_advanced_import_data_successfully), BaseScene.LENGTH_SHORT);
                                } else {
                                    ((SettingsActivity) requireActivity()).showTip(error, BaseScene.LENGTH_SHORT);
                                }
                            });
                        }).start();
                    } catch (Exception e) {
                        ((SettingsActivity) requireActivity()).showTip(e.getLocalizedMessage(), BaseScene.LENGTH_SHORT);
                    }
                }
            }
        } else if (requestCode == REQUEST_DUMP_LOGCAT) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    requireActivity().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        boolean ok = LogCat.save(requireActivity().getContentResolver().openOutputStream(uri));
                        ((SettingsActivity) requireActivity()).showTip(
                                ok ? getString(R.string.settings_advanced_dump_logcat_to, uri.toString()) :
                                        getString(R.string.settings_advanced_dump_logcat_failed), BaseScene.LENGTH_SHORT);
                    } catch (Exception e) {
                        ((SettingsActivity) requireActivity()).showTip(getString(R.string.settings_advanced_dump_logcat_failed), BaseScene.LENGTH_SHORT);
                    }
                }
            }
        }
        super.onActivityResult(group, requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_APP_LANGUAGE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).recreate();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.settings_advanced);
    }
}
