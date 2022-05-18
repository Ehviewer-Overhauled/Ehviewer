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

package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.ViewUtils;

public class SeekBarPanel extends LinearLayout {

    private final int[] mLocation = new int[2];
    private SeekBar mSeekBar;

    public SeekBarPanel(Context context) {
        super(context);
        init();
    }

    public SeekBarPanel(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SeekBarPanel(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SeekBarPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        post(() -> {
            var rootWindowInsets = getRootWindowInsets();
            if (rootWindowInsets != null) {
                setPadding(0, 0, 0, rootWindowInsets.getSystemWindowInsetBottom());
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeekBar = (SeekBar) ViewUtils.$$(this, R.id.seek_bar);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mSeekBar == null) {
            return super.onTouchEvent(event);
        } else {
            ViewUtils.getLocationInAncestor(mSeekBar, mLocation, this);
            final float offsetX = -mLocation[0];
            final float offsetY = -mLocation[1];
            event.offsetLocation(offsetX, offsetY);
            mSeekBar.onTouchEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
            return true;
        }
    }
}
