package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.takisoft.preferencex.PreferenceFragmentCompat;

/**
 * Created by Mo10 on 2018/2/10.
 */

public class PrivacyFragment extends PreferenceFragmentCompat {
    private static final String KEY_PATTERN_PROTECTION = "pattern_protection";

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.privacy_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        Preference patternProtection = findPreference(KEY_PATTERN_PROTECTION);
        patternProtection.setSummary(TextUtils.isEmpty(Settings.getSecurity()) ?
                R.string.settings_privacy_pattern_protection_not_set :
                R.string.settings_privacy_pattern_protection_set);
    }

}
