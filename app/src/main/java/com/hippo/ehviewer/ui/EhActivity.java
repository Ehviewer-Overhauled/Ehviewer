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

package com.hippo.ehviewer.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;

import rikka.core.res.ResourcesKt;
import rikka.insets.WindowInsetsHelper;
import rikka.layoutinflater.view.LayoutInflaterFactory;

public abstract class EhActivity extends AppCompatActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    public static boolean isBlackNightTheme() {
        return Settings.getBoolean("black_dark_theme", false);
    }

    public static String getTheme(Context context) {
        if (isBlackNightTheme() && isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;
        return THEME_DEFAULT;
    }

    public static boolean isNightMode(Configuration configuration) {
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_YES) > 0;
    }

    @StyleRes
    public int getThemeStyleRes(Context context) {
        if (THEME_BLACK.equals(getTheme(context))) {
            return R.style.ThemeOverlay_Black;
        }
        return R.style.ThemeOverlay;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        if (getParent() == null) {
            theme.applyStyle(resid, true);
        } else {
            try {
                theme.setTo(getParent().getTheme());
            } catch (Exception ignored) {
            }
            theme.applyStyle(resid, false);
        }
        theme.applyStyle(getThemeStyleRes(this), true);
        super.onApplyThemeResource(theme, R.style.ThemeOverlay, first);
    }

    @Override
    protected void onNightModeChanged(int mode) {
        getTheme().applyStyle(getThemeStyleRes(this), true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getLayoutInflater().setFactory2((new LayoutInflaterFactory(getDelegate())).addOnViewCreatedListener(WindowInsetsHelper.getLISTENER()));
        super.onCreate(savedInstanceState);


        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);

        window.getDecorView().post(() -> {
            WindowInsets rootWindowInsets = window.getDecorView().getRootWindowInsets();
            if (rootWindowInsets != null && rootWindowInsets.getSystemWindowInsetBottom() >= Resources.getSystem().getDisplayMetrics().density * 40) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.setNavigationBarDividerColor(getColor(R.color.navigation_bar_divider));
                }
                window.setNavigationBarColor(ResourcesKt.resolveColor(getTheme(), android.R.attr.navigationBarColor) & 0x00ffffff | -0x20000000);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.setNavigationBarContrastEnforced(false);
                }
            } else {
                window.setNavigationBarColor(Color.TRANSPARENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.setNavigationBarContrastEnforced(true);
                }
            }
        });

        ((EhApplication) getApplication()).registerActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((EhApplication) getApplication()).unregisterActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Settings.getEnabledSecurity()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
