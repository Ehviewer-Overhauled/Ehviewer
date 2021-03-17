package com.hippo.ehviewer.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.ui.SettingsActivity;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import rikka.recyclerview.RecyclerViewKt;

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
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RecyclerView recyclerView = getListView();
            recyclerView.setClipToPadding(false);
            recyclerView.setClipChildren(false);
            ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
                Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(0, 0, 0, insets1.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
        return recyclerView;
    }
}
