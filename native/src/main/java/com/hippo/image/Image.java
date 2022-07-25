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

package com.hippo.image;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code Image} is a image which stored pixel data in native heap
 */
public final class Image {
    public static final int FORMAT_NORMAL = 0;

    public static final int FORMAT_ANIMATED = 1;

    private static final AtomicInteger sImageCount = new AtomicInteger();
    private final int mFormat;
    private final int mWidth;
    private final int mHeight;
    private long mNativePtr;
    private Throwable mRecycleTracker;

    private Image(long nativePtr, int format, int width, int height) {
        mNativePtr = nativePtr;
        mFormat = format;
        mWidth = width;
        mHeight = height;

        sImageCount.getAndIncrement();
    }

    /**
     * Decode image from {@code InputStream}
     */
    @Nullable
    public static Image decode(FileDescriptor fd, boolean partially) {
        return nativeDecode(fd);
    }

    /**
     * Decode image from {@code InputStream}
     */
    @Nullable
    public static Image decode(Integer fd, boolean partially) {
        return nativeDecodeFdInt(fd);
    }

    /**
     * Decode image from {@code InputStream}
     */
    @Nullable
    public static Image decodeAddr(Long addr, boolean partially) {
        return nativeDecodeAddr(addr);
    }

    /**
     * Create a plain image from Bitmap
     */
    @Nullable
    public static Image create(Bitmap bitmap) {
        return nativeCreate(bitmap);
    }

    /**
     * Return all un-recycled {@code Image} instance count.
     * It is useful for debug.
     */
    public static int getImageCount() {
        return sImageCount.get();
    }

    private static native Image nativeDecode(FileDescriptor fd);

    private static native Image nativeDecodeFdInt(int fd);

    private static native Image nativeDecodeAddr(long addr);

    private static native Image nativeCreate(Bitmap bitmap);

    private static native void nativeRender(long nativePtr,
                                            int srcX, int srcY, Bitmap dst, int dstX, int dstY,
                                            int width, int height, boolean fillBlank, int defaultColor);

    private static native void nativeTexImage(long nativePtr, boolean init, int offsetX, int offsetY, int width, int height);

    private static native void nativeAdvance(long nativePtr);

    private static native int nativeGetDelay(long nativePtr);

    private static native boolean nativeIsOpaque(long nativePtr);

    private static native void nativeRecycle(long nativePtr);

    /**
     * Return the format of the image
     */
    public int getFormat() {
        return mFormat;
    }

    /**
     * Return the width of the image
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Return the height of the image
     */
    public int getHeight() {
        return mHeight;
    }

    private void checkRecycled() {
        if (mNativePtr == 0) {
            if (mRecycleTracker != null) {
                throw new IllegalStateException("The image is recycled.", mRecycleTracker);
            } else {
                throw new IllegalStateException("The image is recycled.");
            }
        }
    }

    /**
     * Render the image to {@code Bitmap}
     */
    public void render(int srcX, int srcY, Bitmap dst, int dstX, int dstY,
                       int width, int height, boolean fillBlank, int defaultColor) {
        checkRecycled();
        nativeRender(mNativePtr, srcX, srcY, dst, dstX, dstY,
                width, height, fillBlank, defaultColor);
    }

    /**
     * Call {@code glTexImage2D} for init is true and
     * call {@code glTexSubImage2D} for init is false.
     * width * height must <= 512 * 512 or do nothing
     */
    public void texImage(boolean init, int offsetX, int offsetY, int width, int height) {
        checkRecycled();
        nativeTexImage(mNativePtr, init, offsetX, offsetY, width, height);
    }

    /**
     * Move to next frame. Do nothing for non-animation image
     */
    public void advance() {
        checkRecycled();
        nativeAdvance(mNativePtr);
    }

    /**
     * Return current frame delay. 0 for non-animation image
     */
    public int getDelay() {
        checkRecycled();
        int delay = nativeGetDelay(mNativePtr);
        return delay <= 10 ? 100 : delay;
    }

    /**
     * Return is the image opaque
     */
    public boolean isOpaque() {
        checkRecycled();
        return nativeIsOpaque(mNativePtr);
    }

    /**
     * Free the native object associated with this image.
     * It must be called when the image will not be used.
     * The image can't be used after this method is called.
     */
    public void recycle() {
        if (mNativePtr != 0) {
            nativeRecycle(mNativePtr);
            mNativePtr = 0;

            sImageCount.getAndDecrement();

            mRecycleTracker = new Throwable("It's a ImageRecycleTracker");
        }
    }

    /**
     * Returns true if this image has been recycled.
     */
    public boolean isRecycled() {
        return mNativePtr == 0;
    }
}
