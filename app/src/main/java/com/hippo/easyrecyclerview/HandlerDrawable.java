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

package com.hippo.easyrecyclerview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class HandlerDrawable extends Drawable {

    private final Paint mPaint;
    private final RectF mTemp = new RectF();
    private int mColor = Color.BLACK;

    public HandlerDrawable() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();

        if (width > height) {
            canvas.drawRect(0, 0, width, height, mPaint);
        } else {
            mTemp.set(0, 0, width, width);
            canvas.drawArc(mTemp, -180, 180, true, mPaint);
            mTemp.set(0, height - width, width, height);
            canvas.drawArc(mTemp, 0, 180, true, mPaint);
            float halfWidth = (float) width / 2.0f;
            canvas.drawRect(0, halfWidth, width, height - halfWidth, mPaint);
        }
    }

    public void setColor(int color) {
        if (mColor != color) {
            mColor = color;
            mPaint.setColor(color);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        int alpha = Color.alpha(mColor);
        if (alpha == 0xff) {
            return PixelFormat.OPAQUE;
        } else if (alpha == 0x00) {
            return PixelFormat.TRANSPARENT;
        } else {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
