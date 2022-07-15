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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.unifile.UniFile;
import com.hippo.util.ExceptionUtils;

public class DownloadFragment extends BasePreferenceFragment {

    public static final String KEY_DOWNLOAD_LOCATION = "download_location";

    @Nullable
    private Preference mDownloadLocation;

    ActivityResultLauncher<Uri> pickImageDirLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            treeUri -> {
                if (treeUri != null) {
                    requireActivity().getContentResolver().takePersistableUriPermission(
                            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    UniFile uniFile = UniFile.fromTreeUri(getActivity(), treeUri);
                    if (uniFile != null) {
                        Settings.putDownloadLocation(uniFile);
                        onUpdateDownloadLocation();
                    } else {
                        showTip(R.string.settings_download_cant_get_download_location,
                                BaseScene.LENGTH_SHORT);
                    }
                }
            });

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.download_settings);

        Preference mediaScan = findPreference(Settings.KEY_MEDIA_SCAN);
        Preference imageResolution = findPreference(Settings.KEY_IMAGE_RESOLUTION);
        mDownloadLocation = findPreference(KEY_DOWNLOAD_LOCATION);

        onUpdateDownloadLocation();

        mediaScan.setOnPreferenceChangeListener(this);
        imageResolution.setOnPreferenceChangeListener(this);

        if (mDownloadLocation != null) {
            mDownloadLocation.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloadLocation = null;
    }

    public void onUpdateDownloadLocation() {
        UniFile file = Settings.getDownloadLocation();
        if (mDownloadLocation != null) {
            if (file != null) {
                mDownloadLocation.setSummary(file.getUri().toString());
            } else {
                mDownloadLocation.setSummary(R.string.settings_download_invalid_download_location);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_DOWNLOAD_LOCATION.equals(key)) {
            UniFile file = Settings.getDownloadLocation();
            if (file != null && !UniFile.isFileUri(Settings.getDownloadLocation().getUri())) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_download_download_location)
                        .setMessage(file.getUri().toString())
                        .setPositiveButton(R.string.pick_new_download_location, (dialogInterface, i) -> openDirPickerL())
                        .setNeutralButton(R.string.reset_download_location, (dialogInterface, i) -> {
                            UniFile uniFile = UniFile.fromFile(AppConfig.getDefaultDownloadDir());
                            if (uniFile != null) {
                                Settings.putDownloadLocation(uniFile);
                                onUpdateDownloadLocation();
                            } else {
                                showTip(R.string.settings_download_cant_get_download_location,
                                        BaseScene.LENGTH_SHORT);
                            }
                        })
                        .show();
            } else {
                openDirPickerL();
            }
            return true;
        }
        return false;
    }

    private void openDirPickerL() {
        try {
            pickImageDirLauncher.launch(null);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            showTip(R.string.error_cant_find_activity, BaseScene.LENGTH_SHORT);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (Settings.KEY_MEDIA_SCAN.equals(key)) {
            if (newValue instanceof Boolean) {
                UniFile downloadLocation = Settings.getDownloadLocation();
                if ((Boolean) newValue) {
                    CommonOperations.removeNoMediaFile(downloadLocation);
                } else {
                    CommonOperations.ensureNoMediaFile(downloadLocation);
                }
            }
            return true;
        } else if (Settings.KEY_IMAGE_RESOLUTION.equals(key)) {
            if (newValue instanceof String) {
                Settings.putImageResolution((String) newValue);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getFragmentTitle() {
        return R.string.settings_download;
    }
}
