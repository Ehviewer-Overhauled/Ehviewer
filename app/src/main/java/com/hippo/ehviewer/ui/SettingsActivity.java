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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.fragment.AboutFragment;
import com.hippo.ehviewer.ui.fragment.AdvancedFragment;
import com.hippo.ehviewer.ui.fragment.DownloadFragment;
import com.hippo.ehviewer.ui.fragment.EhFragment;
import com.hippo.ehviewer.ui.fragment.PrivacyFragment;
import com.hippo.ehviewer.ui.fragment.ReadFragment;
import com.hippo.ehviewer.ui.fragment.SettingsFragment;
import com.hippo.util.DrawableManager;

public final class SettingsActivity extends EhActivity {

    private static final int REQUEST_CODE_FRAGMENT = 0;

    private static final String[] ENTRY_FRAGMENTS = {
            EhFragment.class.getName(),
            ReadFragment.class.getName(),
            DownloadFragment.class.getName(),
            AdvancedFragment.class.getName(),
            AboutFragment.class.getName(),
            PrivacyFragment.class.getName(),
    };

    @Override
    protected int getThemeResId(int theme) {
        return R.style.AppTheme_Settings;
    }

    private void setActionBarUpIndicator(Drawable drawable) {
        ActionBarDrawerToggle.Delegate delegate = getDrawerToggleDelegate();
        if (delegate != null) {
            delegate.setActionBarUpIndicator(drawable, 0);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, new SettingsFragment())
                    .commitAllowingStateLoss();
        }
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setActionBarUpIndicator(DrawableManager.getVectorDrawable(this, R.drawable.v_arrow_left_dark_x24));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FRAGMENT) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
