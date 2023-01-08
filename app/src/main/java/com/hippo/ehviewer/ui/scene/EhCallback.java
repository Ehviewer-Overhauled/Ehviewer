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

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.ui.EhActivity;
import com.hippo.ehviewer.ui.MainActivity;

public abstract class EhCallback<E extends Fragment, T> implements EhClient.Callback<T> {

    private final EhApplication mApplication;

    public EhCallback(Context context) {
        mApplication = (EhApplication) context.getApplicationContext();
    }

    public Context getContent() {
        Context context = mApplication.getTopActivity();
        if (context == null) {
            context = getApplication();
        }
        return context;
    }

    public EhApplication getApplication() {
        return mApplication;
    }

    public void showTip(@StringRes int id, int length) {
        EhActivity activity = mApplication.getTopActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(id, length);
        } else {
            Toast.makeText(getApplication(), id,
                    length == BaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    public void showTip(String tip, int length) {
        EhActivity activity = mApplication.getTopActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(tip, length);
        } else {
            Toast.makeText(getApplication(), tip,
                    length == BaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }
}
