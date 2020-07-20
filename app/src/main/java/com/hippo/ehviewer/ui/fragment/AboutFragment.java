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

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.util.AppHelper;
import com.microsoft.appcenter.distribute.Distribute;
import com.takisoft.preferencex.PreferenceFragmentCompat;

public class AboutFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_DONATE = "donate";
    private static final String KEY_CHECK_FOR_UPDATES = "check_for_updates";

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.about_settings);

        Preference author = findPreference(KEY_AUTHOR);
        //Preference donate = findPreference(KEY_DONATE);
        Preference checkForUpdate = findPreference(KEY_CHECK_FOR_UPDATES);

        author.setSummary(getString(R.string.settings_about_author_summary).replace('$', '@'));

        author.setOnPreferenceClickListener(this);
        //donate.setOnPreferenceClickListener(this);
        checkForUpdate.setOnPreferenceClickListener(this);
        checkForUpdate.setVisible(Analytics.isEnabled());
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.license));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_AUTHOR.equals(key)) {
            AppHelper.sendEmail(requireActivity(), EhApplication.getDeveloperEmail(),
                    "About EhViewer", null);
        } else if (KEY_CHECK_FOR_UPDATES.equals(key)) {
            Distribute.checkForUpdate();
        }
        return true;
    }
}
