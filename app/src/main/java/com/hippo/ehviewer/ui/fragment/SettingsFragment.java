package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.hippo.ehviewer.R;
import com.takisoft.preferencex.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

        addPreferencesFromResource(R.xml.settings_headers);
    }
}
