/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.tuxiang;

import javax.microedition.khronos.opengles.GL;

// android-9.0.0_r30

public interface GLStuff {

    boolean LOG_ATTACH_DETACH = false;
    boolean LOG_THREADS = false;
    boolean LOG_PAUSE_RESUME = false;
    boolean LOG_SURFACE = false;
    boolean LOG_RENDERER = false;
    boolean LOG_RENDERER_DRAW_FRAME = false;
    boolean LOG_EGL = false;

    /**
     * The renderer only renders
     * when the surface is created, or when {@link #requestRender} is called.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     * @see #requestRender()
     */
    int RENDERMODE_WHEN_DIRTY = 0;

    /**
     * The renderer is called
     * continuously to re-render the scene.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     */
    int RENDERMODE_CONTINUOUSLY = 1;

    /**
     * Check glError() after every GL call and throw an exception if glError indicates
     * that an error has occurred. This can be used to help track down which OpenGL ES call
     * is causing an error.
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    int DEBUG_CHECK_GL_ERROR = 1;

    /**
     * Log GL calls to the system log at "verbose" level with tag "GLSurfaceView".
     *
     * @see #getDebugFlags
     * @see #setDebugFlags
     */
    int DEBUG_LOG_GL_CALLS = 2;

    /**
     * Get the native window object
     */
    Object getNativeWindow();

    /**
     * Set the glWrapper. If the glWrapper is not null, its
     * {@link GLWrapper#wrap(GL)} method is called
     * whenever a surface is created. A GLWrapper can be used to wrap
     * the GL object that's passed to the renderer. Wrapping a GL
     * object enables examining and modifying the behavior of the
     * GL calls made by the renderer.
     * <p>
     * Wrapping is typically used for debugging purposes.
     * <p>
     * The default value is null.
     * @param glWrapper the new GLWrapper
     */
    void setGLWrapper(GLWrapper glWrapper);

    /**
     * @return Returns the {@code GLWrapper}
     */
    GLWrapper getGLWrapper();

    /**
     * Set the debug flags to a new value. The value is
     * constructed by OR-together zero or more
     * of the DEBUG_CHECK_* constants. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     * @param debugFlags the new debug flags
     * @see #DEBUG_CHECK_GL_ERROR
     * @see #DEBUG_LOG_GL_CALLS
     */
    void setDebugFlags(int debugFlags);

    /**
     * Get the current value of the debug flags.
     * @return the current value of the debug flags.
     */
    int getDebugFlags();

    /**
     * Control whether the EGL context is preserved when the GLSurfaceView is paused and
     * resumed.
     * <p>
     * If set to true, then the EGL context may be preserved when the GLSurfaceView is paused.
     * <p>
     * Prior to API level 11, whether the EGL context is actually preserved or not
     * depends upon whether the Android device can support an arbitrary number of
     * EGL contexts or not. Devices that can only support a limited number of EGL
     * contexts must release the EGL context in order to allow multiple applications
     * to share the GPU.
     * <p>
     * If set to false, the EGL context will be released when the GLSurfaceView is paused,
     * and recreated when the GLSurfaceView is resumed.
     * <p>
     *
     * The default is false.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    void setPreserveEGLContextOnPause(boolean preserveOnPause);

    /**
     * @return true if the EGL context will be preserved when paused
     */
    boolean getPreserveEGLContextOnPause();

    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a GLSurfaceView.
     * <p>The following GLSurfaceView methods can only be called <em>before</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
     * </ul>
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    void setRenderer(Renderer renderer);

    /**
     * @return Returns the {@code Renderer}
     */
    Renderer getRenderer();

    /**
     * Install a custom EGLContextFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    void setEGLContextFactory(EGLContextFactory factory);

    /**
     * @return Returns the {@code EGLWindowSurfaceFactory}
     */
    EGLWindowSurfaceFactory getEGLWindowSurfaceFactory();

    /**
     * Install a custom EGLWindowSurfaceFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a window surface will be created with a null attribute list.
     */
    void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory);

    /**
     * @return Returns the {@code EGLContextFactory}
     */
    EGLContextFactory getEGLContextFactory();

    /**
     * Install a custom EGLConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an EGLConfig that is compatible with the current
     * android.view.Surface, with a depth buffer depth of
     * at least 16 bits.
     */
    void setEGLConfigChooser(EGLConfigChooser configChooser);

    /**
     * @return Returns the {@code EGLConfigChooser}
     */
    EGLConfigChooser getEGLConfigChooser();

    /**
     * Inform the default EGLContextFactory and default EGLConfigChooser
     * which EGLContext client version to pick.
     * <p>Use this method to create an OpenGL ES 2.0-compatible context.
     * Example:
     * <pre class="prettyprint">
     *     public MyView(Context context) {
     *         super(context);
     *         setEGLContextClientVersion(2); // Pick an OpenGL ES 2.0 context.
     *         setRenderer(new MyRenderer());
     *     }
     * </pre>
     * <p>Note: Activities which require OpenGL ES 2.0 should indicate this by
     * setting @lt;uses-feature android:glEsVersion="0x00020000" /> in the activity's
     * AndroidManifest.xml file.
     * <p>If this method is called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>This method only affects the behavior of the default EGLContexFactory and the
     * default EGLConfigChooser. If
     * {@link #setEGLContextFactory(EGLContextFactory)} has been called, then the supplied
     * EGLContextFactory is responsible for creating an OpenGL ES 2.0-compatible context.
     * If
     * {@link #setEGLConfigChooser(EGLConfigChooser)} has been called, then the supplied
     * EGLConfigChooser is responsible for choosing an OpenGL ES 2.0-compatible config.
     * @param version The EGLContext client version to choose. Use 2 for OpenGL ES 2.0
     */
    void setEGLContextClientVersion(int version);

    /**
     * Get EGLContextClientVersion
     */
    int getEGLContextClientVersion();

    /**
     * Set the rendering mode. When renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RENDERMODE_CONTINUOUSLY.
     * <p>
     * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(Renderer)}
     *
     * @param renderMode one of the RENDERMODE_X constants
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    void setRenderMode(int renderMode);

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     * @return the current rendering mode.
     * @see #RENDERMODE_CONTINUOUSLY
     * @see #RENDERMODE_WHEN_DIRTY
     */
    int getRenderMode();

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link #RENDERMODE_WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    void requestRender();

    /**
     * Pause the rendering thread, optionally tearing down the EGL context
     * depending upon the value of {@link #setPreserveEGLContextOnPause(boolean)}.
     *
     * This method should be called when it is no longer desirable for the
     * GLSurfaceView to continue rendering, such as in response to
     * {@link android.app.Activity#onStop Activity.onStop}.
     *
     * Must not be called before a renderer has been set.
     */
    void onPause();

    /**
     * Resumes the rendering thread, re-creating the OpenGL context if necessary. It
     * is the counterpart to {@link #onPause()}.
     *
     * This method should typically be called in
     * {@link android.app.Activity#onStart Activity.onStart}.
     *
     * Must not be called before a renderer has been set.
     */
    void onResume();

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     * @param r the runnable to be run on the GL rendering thread.
     */
    void queueEvent(Runnable r);
}
