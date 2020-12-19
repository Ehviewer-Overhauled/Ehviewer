package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.hippo.ehviewer.R;

public class SettingsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_headers);
    }

    @Override
    public int getFragmentTitle() {
        return R.string.settings;
    }
}
