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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.snackbar.Snackbar;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.fragment.SettingsFragment;
import com.hippo.ehviewer.ui.scene.BaseScene;

public final class SettingsActivity extends EhActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, new SettingsFragment())
                    .commitAllowingStateLoss();
        }
    }

    public void showTip(@StringRes int id, int length) {
        showTip(getString(id), length);
    }

    public void showTip(CharSequence message, int length) {
        Snackbar.make(findViewById(R.id.snackbar), message,
                length == BaseScene.LENGTH_LONG ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
