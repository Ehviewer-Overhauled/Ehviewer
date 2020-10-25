package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.hippo.ehviewer.R;

public class SettingsFragment extends BaseSettingsFragment {
    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_headers);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.settings);
    }
}
