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

package com.hippo.drawable;

import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;

import com.hippo.widget.ObservedTextView;

import coil.Coil;
import coil.request.Disposable;
import coil.request.ImageRequest;

public class UnikeryDrawable extends WrapDrawable implements ObservedTextView.OnWindowAttachListener {

    private static final String TAG = UnikeryDrawable.class.getSimpleName();
    private final ObservedTextView mTextView;
    private String mUrl;
    private Disposable task;

    public UnikeryDrawable(ObservedTextView textView) {
        mTextView = textView;
        mTextView.setOnWindowAttachListener(this);
    }

    @Override
    public void onAttachedToWindow() {
        load(mUrl);
    }

    @Override
    public void onDetachedFromWindow() {
        if (task != null && !task.isDisposed())
            task.dispose();
        clearDrawable();
    }

    public void load(String url) {
        if (url != null) {
            mUrl = url;
            var imageLoader = Coil.imageLoader(mTextView.getContext());
            var request = new ImageRequest.Builder(mTextView.getContext()).data(url).memoryCacheKey(url).diskCacheKey(url).target(
                    drawable -> null,
                    drawable -> null,
                    drawable -> {
                        onGetValue(drawable);
                        return null;
                    }
            ).build();
            task = imageLoader.enqueue(request);
        }
    }

    private void clearDrawable() {
        setDrawable(null);
    }

    @Override
    public void setDrawable(Drawable drawable) {
        // Remove old callback
        Drawable oldDrawable = getDrawable();
        if (oldDrawable != null) {
            oldDrawable.setCallback(null);
        }

        super.setDrawable(drawable);

        if (drawable != null) {
            drawable.setCallback(mTextView);
        }

        updateBounds();
        if (drawable != null) {
            invalidateSelf();
        }
    }

    @Override
    public void invalidateSelf() {
        CharSequence cs = mTextView.getText();
        mTextView.setText(cs);
    }

    public void onGetValue(Drawable drawable) {
        clearDrawable();
        setDrawable(drawable);
        if (drawable instanceof AnimatedImageDrawable animatedImageDrawable)
            animatedImageDrawable.start();
    }
}
