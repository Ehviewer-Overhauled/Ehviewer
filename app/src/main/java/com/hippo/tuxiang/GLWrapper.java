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

import javax.microedition.khronos.opengles.GL;

// android-9.0.0_r30

/**
 * An interface used to wrap a GL interface.
 * <p>Typically
 * used for implementing debugging and tracing on top of the default
 * GL interface. You would typically use this by creating your own class
 * that implemented all the GL methods by delegating to another GL instance.
 * Then you could add your own behavior before or after calling the
 * delegate. All the GLWrapper would do was instantiate and return the
 * wrapper GL instance:
 * <pre class="prettyprint">
 * class MyGLWrapper implements GLWrapper {
 *     GL wrap(GL gl) {
 *         return new MyGLImplementation(gl);
 *     }
 *     static class MyGLImplementation implements GL,GL10,GL11,... {
 *         ...
 *     }
 * }
 * </pre>
 */
public interface GLWrapper {
    /**
     * Wraps a gl interface in another gl interface.
     * @param gl a GL interface that is to be wrapped.
     * @return either the input argument or another GL object that wraps the input argument.
     */
    GL wrap(GL gl);
}
