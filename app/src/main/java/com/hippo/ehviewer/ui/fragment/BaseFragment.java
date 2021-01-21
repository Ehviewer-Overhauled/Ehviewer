package com.hippo.ehviewer.ui.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.ui.SettingsActivity;

public class BaseFragment extends Fragment {
    @Override
    public void onStart() {
        super.onStart();
        setTitle(getFragmentTitle());
    }

    @StringRes
    public int getFragmentTitle() {
        return -1;
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
            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(0, 0, 0, insets1.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }
}
