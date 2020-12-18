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

package com.hippo.ehviewer;

import android.app.Application;

import com.hippo.scene.SceneFragment;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;

import java.util.HashMap;
import java.util.Map;

public final class Analytics {

    public static void start(Application application) {
        AppCenter.start(application, "1aa06805-037c-4f3c-8aaa-e23d1433986d", com.microsoft.appcenter.analytics.Analytics.class, Crashes.class, Distribute.class);
        AppCenter.setUserId(Settings.getUserID());
        Crashes.setEnabled(!BuildConfig.DEBUG);
        Distribute.setEnabledForDebuggableBuild(false);
    }

    public static boolean isEnabled() {
        return AppCenter.isConfigured() && Settings.getEnableAnalytics();
    }

    public static void onSceneView(SceneFragment scene) {
        if (isEnabled()) {
            Map<String, String> properties = new HashMap<>();
            properties.put("scene_simple_class", scene.getClass().getSimpleName());
            properties.put("scene_class", scene.getClass().getName());
            com.microsoft.appcenter.analytics.Analytics.trackEvent("scene_view", properties);
        }
    }
}