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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// android-9.0.0_r30

/**
 * A generic renderer interface.
 * <p>
 * The renderer is responsible for making OpenGL calls to render a frame.
 * <p>
 * GLSurfaceView clients typically create their own classes that implement
 * this interface, and then call {@link GLStuff#setRenderer} to
 * register the renderer with the GLSurfaceView.
 * <p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about how to use OpenGL, read the
 * <a href="{@docRoot}guide/topics/graphics/opengl.html">OpenGL</a> developer guide.</p>
 * </div>
 *
 * <h3>Threading</h3>
 * The renderer will be called on a separate thread, so that rendering
 * performance is decoupled from the UI thread. Clients typically need to
 * communicate with the renderer from the UI thread, because that's where
 * input events are received. Clients can communicate using any of the
 * standard Java techniques for cross-thread communication, or they can
 * use the {@link GLStuff#queueEvent(Runnable)} convenience method.
 * <p>
 * <h3>EGL Context Lost</h3>
 * There are situations where the EGL rendering context will be lost. This
 * typically happens when device wakes up after going to sleep. When
 * the EGL context is lost, all OpenGL resources (such as textures) that are
 * associated with that context will be automatically deleted. In order to
 * keep rendering correctly, a renderer must recreate any lost resources
 * that it still needs. The {@link #onSurfaceCreated(GL10, EGLConfig)} method
 * is a convenient place to do this.
 *
 * @see GLStuff#setRenderer(Renderer)
 */
public interface Renderer {
    /**
     * Called when the surface is created or recreated.
     * <p>
     * Called when the rendering thread
     * starts and whenever the EGL context is lost. The EGL context will typically
     * be lost when the Android device awakes after going to sleep.
     * <p>
     * Since this method is called at the beginning of rendering, as well as
     * every time the EGL context is lost, this method is a convenient place to put
     * code to create resources that need to be created when the rendering
     * starts, and that need to be recreated when the EGL context is lost.
     * Textures are an example of a resource that you might want to create
     * here.
     * <p>
     * Note that when the EGL context is lost, all OpenGL resources associated
     * with that context will be automatically deleted. You do not need to call
     * the corresponding "glDelete" methods such as glDeleteTextures to
     * manually delete these lost resources.
     * <p>
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     * @param config the EGLConfig of the created surface. Can be used
     * to create matching pbuffers.
     */
    void onSurfaceCreated(GL10 gl, EGLConfig config);

    /**
     * Called when the surface changed size.
     * <p>
     * Called after the surface is created and whenever
     * the OpenGL ES surface size changes.
     * <p>
     * Typically you will set your viewport here. If your camera
     * is fixed then you could also set your projection matrix here:
     * <pre class="prettyprint">
     * void onSurfaceChanged(GL10 gl, int width, int height) {
     *     gl.glViewport(0, 0, width, height);
     *     // for a fixed camera, set the projection too
     *     float ratio = (float) width / height;
     *     gl.glMatrixMode(GL10.GL_PROJECTION);
     *     gl.glLoadIdentity();
     *     gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
     * }
     * </pre>
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     */
    void onSurfaceChanged(GL10 gl, int width, int height);

    /**
     * Called to draw the current frame.
     * <p>
     * This method is responsible for drawing the current frame.
     * <p>
     * The implementation of this method typically looks like this:
     * <pre class="prettyprint">
     * void onDrawFrame(GL10 gl) {
     *     gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
     *     //... other gl calls to render the scene ...
     * }
     * </pre>
     * @param gl the GL interface. Use <code>instanceof</code> to
     * test if the interface supports GL11 or higher interfaces.
     * @return {@code false} for not drew, will not swap buffers.
     */
    boolean onDrawFrame(GL10 gl);

    /**
     * Called when the GL thread exits.
     * <p>
     * GL thread starts after {@link GLStuff#setRenderer(Renderer)} called, and
     * after {@link android.view.View#onDetachedFromWindow} called except
     * the first time.
     * <p>
     * It is a good time to prepare resources.
     */
    void onGLThreadStart();

    /**
     * Called when the GL thread exits.
     * <p>
     * GL thread ends after {@link android.view.View#onDetachedFromWindow} called.
     * <p>
     * It is a good time to release resources.
     */
    void onGLThreadExit();

    /**
     * Called after {@link GLStuff#onPause()} called.
     * <p>
     * It is a good time to stop animations.
     */
    void onGLThreadPause();

    /**
     * Called after {@link GLStuff#onResume()} called.
     * <p>
     * It is a good time to restart animations.
     */
    void onGLThreadResume();
}
