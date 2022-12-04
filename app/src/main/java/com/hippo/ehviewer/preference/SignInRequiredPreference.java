/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.ehviewer.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.ui.SettingsActivity;
import com.hippo.ehviewer.ui.scene.BaseScene;

import okhttp3.HttpUrl;

public class SignInRequiredPreference extends Preference implements Preference.OnPreferenceClickListener {

    @SuppressLint("StaticFieldLeak")
    private SettingsActivity mActivity;

    public SignInRequiredPreference(Context context) {
        super(context);
        init(context);
    }

    public SignInRequiredPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SignInRequiredPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mActivity = (SettingsActivity) context;
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        EhCookieStore store = EhApplication.getEhCookieStore();
        HttpUrl e = HttpUrl.get(EhUrl.HOST_E);
        HttpUrl ex = HttpUrl.get(EhUrl.HOST_EX);

        if (store.contains(e, EhCookieStore.KEY_IPD_MEMBER_ID) ||
                store.contains(e, EhCookieStore.KEY_IPD_PASS_HASH) ||
                store.contains(ex, EhCookieStore.KEY_IPD_MEMBER_ID) ||
                store.contains(ex, EhCookieStore.KEY_IPD_PASS_HASH)) {
            return false;
        } else {
            mActivity.showTip(R.string.error_please_login_first, BaseScene.LENGTH_SHORT);
            return true;
        }
    }
}
