package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;

import com.takisoft.preferencex.PreferenceFragmentCompat;

public class BaseSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    }

    public void setTitle(@StringRes int string) {
        requireActivity().setTitle(string);
    }
}
