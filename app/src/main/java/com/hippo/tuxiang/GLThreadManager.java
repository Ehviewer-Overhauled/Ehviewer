/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.tuxiang;

import android.util.Log;

// android-9.0.0_r30

final class GLThreadManager {
    private static final String TAG = "GLThreadManager";

    public synchronized void threadExiting(GLThread thread) {
        if (GLStuff.LOG_THREADS) {
            Log.i("GLThread", "exiting tid=" +  thread.getId());
        }
        thread.mExited = true;
        notifyAll();
    }

    /*
     * Releases the EGL context. Requires that we are already in the
     * sGLThreadManager monitor when this is called.
     */
    public void releaseEglContextLocked(GLThread thread) {
        notifyAll();
    }
}
