/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.SettingsActivity;


public class BasePreferenceFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    @Override
    public void onStart() {
        super.onStart();
        setTitle(getFragmentTitle());
    }

    @StringRes
    public int getFragmentTitle() {
        return -1;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {

    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        try {
            Fragment classObj = (Fragment) Class.forName(key).newInstance();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fragment, classObj)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (ClassNotFoundException | IllegalAccessException |
                 java.lang.InstantiationException ignored) {
        }
        return true;
    }

    private void setTitle(@StringRes int string) {
        requireActivity().setTitle(string);
    }

    public void showTip(@StringRes int id, int length) {
        ((SettingsActivity) requireActivity()).showTip(getString(id), length);
    }

    public void showTip(CharSequence message, int length) {
        ((SettingsActivity) requireActivity()).showTip(message, length);
    }
}
