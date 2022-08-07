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
import java.util.ArrayList;

/**
 * The {@code Image} is a image which stored pixel data in native heap
 */
public final class Image {
    public static final int FORMAT_NORMAL = 0;
    public static final int FORMAT_ANIMATED = 1;
    private static final ArrayList<Image> imageList = new ArrayList<>();
    private static boolean needEvictAll = false;
    private long mNativePtr;

    private Image(long nativePtr) {
        if (needEvictAll)
            evictAll();
        mNativePtr = nativePtr;
        imageList.add(this);
    }

    private static Image newFromAddr(long addr) {
        if (addr == 0)
            return null;
        return new Image(addr);
    }

    public static void lazyEvictAll() {
        needEvictAll = true;
    }

    private static void evictAll() {
        (new ArrayList<>(imageList)).forEach(Image::recycle);
        needEvictAll = false;
    }

    /**
     * Decode image from {@code InputStream}
     */
    @Nullable
    public static Image decode(FileDescriptor fd, boolean partially) {
        return newFromAddr(nativeDecode(fd));
    }

    /**
     * Decode image from {@code InputStream}
     */
    @Nullable
    public static Image decode(Integer fd, boolean partially) {
        return newFromAddr(nativeDecodeFdInt(fd));
    }

    /**
     * Create a plain image from Bitmap
     */
    @Nullable
    public static Image create(Bitmap bitmap) {
        return newFromAddr(nativeCreate(bitmap));
    }

    /**
     * Return all un-recycled {@code Image} instance count.
     * It is useful for debug.
     */
    public static int getImageCount() {
        return imageList.size();
    }

    private static native long nativeDecode(FileDescriptor fd);

    private static native long nativeDecodeFdInt(int fd);

    private static native long nativeCreate(Bitmap bitmap);

    private static native void nativeRender(long nativePtr,
                                            int srcX, int srcY, Bitmap dst, int dstX, int dstY,
                                            int width, int height);

    private static native void nativeTexImage(long nativePtr, boolean init, int offsetX, int offsetY, int width, int height);

    private static native void nativeAdvance(long nativePtr);

    private static native int nativeGetDelay(long nativePtr);

    private static native boolean nativeIsOpaque(long nativePtr);

    private static native void nativeRecycle(long nativePtr);

    private static native int nativeGetFormat(long nativePtr);

    private static native int nativeGetWidth(long nativePtr);

    private static native int nativeGetHeight(long nativePtr);

    /**
     * Return the format of the image
     */
    public int getFormat() {
        checkRecycled();
        return nativeGetFormat(mNativePtr);
    }

    /**
     * Return the width of the image
     */
    public int getWidth() {
        checkRecycled();
        return nativeGetWidth(mNativePtr);
    }

    /**
     * Return the height of the image
     */
    public int getHeight() {
        checkRecycled();
        return nativeGetHeight(mNativePtr);
    }

    private void checkRecycled() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("The image is recycled.");
        }
    }

    /**
     * Render the image to {@code Bitmap}
     */
    public void render(int srcX, int srcY, Bitmap dst, int dstX, int dstY,
                       int width, int height, boolean fillBlank, int defaultColor) {
        checkRecycled();
        nativeRender(mNativePtr, srcX, srcY, dst, dstX, dstY, width, height);
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
    public synchronized void recycle() {
        if (mNativePtr != 0) {
            nativeRecycle(mNativePtr);
            mNativePtr = 0;
            imageList.remove(this);
        }
    }

    /**
     * Returns true if this image has been recycled.
     */
    public boolean isRecycled() {
        return mNativePtr == 0;
    }
}
