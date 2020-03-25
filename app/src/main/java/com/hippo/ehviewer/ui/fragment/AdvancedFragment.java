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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.util.LogCat;
import com.hippo.util.ReadableTime;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.io.File;
import java.util.Arrays;

public class AdvancedFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
    private static final int REQUEST_DUMP_LOGCAT = 3;

    private static final String KEY_DUMP_LOGCAT = "dump_logcat";
    private static final String KEY_CLEAR_MEMORY_CACHE = "clear_memory_cache";
    private static final String KEY_APP_LANGUAGE = "app_language";
    private static final String KEY_IMPORT_DATA = "import_data";
    private static final String KEY_EXPORT_DATA = "export_data";

    private static void importData(final Context context) {
        final File dir = AppConfig.getExternalDataDir();
        if (null == dir) {
            Toast.makeText(context, R.string.cant_get_data_dir, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] files = dir.list();
        if (null == files || files.length <= 0) {
            Toast.makeText(context, R.string.cant_find_any_data, Toast.LENGTH_SHORT).show();
            return;
        }
        Arrays.sort(files);
        new MaterialAlertDialogBuilder(context).setItems(files, (dialog, which) -> {
            File file = new File(dir, files[which]);
            //String error = EhDB.importDB(context, file);
            //if (null == error) {
            //    error = context.getString(R.string.settings_advanced_import_data_successfully);
            //}
            //Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        }).show();
    }

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
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DUMP_LOGCAT.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt");
            startActivityForResult(intent, REQUEST_DUMP_LOGCAT);
            return true;
        } else if (KEY_CLEAR_MEMORY_CACHE.equals(key)) {
            ((EhApplication) getActivity().getApplication()).clearMemoryCache();
            Runtime.getRuntime().gc();
        } else if (KEY_IMPORT_DATA.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
            return true;
        } else if (KEY_EXPORT_DATA.equals(key)) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-sqlite3");
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
                                Toast.makeText(requireContext(),
                                        (success)
                                                ? GetText.getString(R.string.settings_advanced_export_data_to, uri.toString())
                                                : GetText.getString(R.string.settings_advanced_export_data_failed),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    } catch (Exception e) {
                        GetText.getString(R.string.settings_advanced_export_data_failed);
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
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
                                    Toast.makeText(requireContext(), getString(R.string.settings_advanced_import_data_successfully), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (requestCode == REQUEST_DUMP_LOGCAT) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        boolean ok = LogCat.save(requireActivity().getContentResolver().openOutputStream(uri));
                        Toast.makeText(getActivity(),
                                ok ? getString(R.string.settings_advanced_dump_logcat_to, uri.toString()) :
                                        getString(R.string.settings_advanced_dump_logcat_failed), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), getString(R.string.settings_advanced_dump_logcat_failed), Toast.LENGTH_SHORT).show();
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
}
