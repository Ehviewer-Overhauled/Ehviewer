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

package com.hippo.scene;

import android.app.assist.AssistContent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.collect.IntList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import rikka.core.res.ResourcesKt;

public class SceneFragment extends Fragment {

    public static final int LAUNCH_MODE_STANDARD = 0;
    public static final int LAUNCH_MODE_SINGLE_TOP = 1;
    public static final int LAUNCH_MODE_SINGLE_TASK = 2;
    /**
     * Standard scene result: operation canceled.
     */
    public static final int RESULT_CANCELED = 0;
    /**
     * Standard scene result: operation succeeded.
     */
    public static final int RESULT_OK = -1;

    public void onNewArguments(@NonNull Bundle args) {
    }

    public void startScene(Announcer announcer, boolean horizontal) {

    }

    public void startScene(Announcer announcer) {

    }

    public void finish() {

    }

    public void onProvideAssistContent(AssistContent outContent) {

    }

    protected void onSceneResult(int requestCode, int resultCode, Bundle data) {
    }

    public void setResult(int resultCode, Bundle result) {

    }

    @IntDef({LAUNCH_MODE_STANDARD, LAUNCH_MODE_SINGLE_TOP, LAUNCH_MODE_SINGLE_TASK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LaunchMode {
    }
}
