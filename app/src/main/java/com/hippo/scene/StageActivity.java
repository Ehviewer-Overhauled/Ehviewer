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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.ui.EhActivity;

public abstract class StageActivity extends EhActivity {

    public static final String ACTION_START_SCENE = "start_scene";
    public static final String KEY_SCENE_NAME = "stage_activity_scene_name";
    public static final String KEY_SCENE_ARGS = "stage_activity_scene_args";

    public static void registerLaunchMode(Class<?> clazz, @SceneFragment.LaunchMode int launchMode) {
    }

    /**
     * Start scene from {@code Intent}, it might be not safe,
     * Correct it here.
     *
     * @return {@code null} for do not start scene
     */
    @Nullable
    protected Announcer onStartSceneFromIntent(@NonNull Class<?> clazz, @Nullable Bundle args) {
        return new Announcer(clazz).setArgs(args);
    }


    /**
     * Can't recognize intent in first time {@code onCreate} and {@code onNewIntent},
     * null included.
     */
    protected void onUnrecognizedIntent(@Nullable Intent intent) {
    }

    void onSceneDestroyed(SceneFragment scene) {
    }

    protected void onRegister(int id) {
    }

    protected void onUnregister() {
    }

    protected void onTransactScene() {
    }

    public int getStageId() {
        return 0;
    }

    public void startScene(Announcer announcer) {
    }

    public void startScene(Announcer announcer, boolean horizontal) {
    }

    public void startSceneFirstly(Announcer announcer) {
    }

    public void finishScene(SceneFragment scene) {
    }

    public void refreshTopScene() {
    }

    public SceneFragment findSceneByTag(String tag) {
        return null;
    }

    @Nullable
    public Class<?> getTopSceneClass() {
        return null;
    }
}
