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

package com.hippo.ehviewer.ui.scene;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.yorozuya.ViewUtils;

public class SelectSiteScene extends SolidScene implements View.OnClickListener {

    private MaterialButtonToggleGroup mButtonGroup;
    private View mOk;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_select_site, container, false);

        mButtonGroup = (MaterialButtonToggleGroup) ViewUtils.$$(view, R.id.button_group);
        if (EhApplication.getEhCookieStore().hasSignedIn()) {
            ((MaterialButton) view.findViewById(R.id.site_ex)).setChecked(true);
        } else {
            ((MaterialButton) view.findViewById(R.id.site_e)).setChecked(true);
        }
        mOk = ViewUtils.$$(view, R.id.ok);

        mOk.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mButtonGroup = null;
        mOk = null;
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = getMainActivity();
        if (null == activity || null == mButtonGroup) {
            return;
        }

        if (v == mOk) {
            int id = mButtonGroup.getCheckedButtonId();
            if (id == R.id.site_e) {
                Settings.putSelectSite(false);
                Settings.putGallerySite(EhUrl.SITE_E);
                startSceneForCheckStep(CHECK_STEP_SELECT_SITE, getArguments());
                finish();
            } else if (id == R.id.site_ex) {
                Settings.putSelectSite(false);
                Settings.putGallerySite(EhUrl.SITE_EX);
                startSceneForCheckStep(CHECK_STEP_SELECT_SITE, getArguments());
                finish();
            } else {
                showTip(R.string.no_select, LENGTH_SHORT);
            }
        }
    }
}
