/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.glview.glrenderer;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.hippo.yorozuya.AssertUtils;

public class SpriteTexture extends TiledTexture {

    private final int mCount;
    private final int[] mRects;

    private final RectF mTempSource = new RectF();
    private final RectF mTempTarget = new RectF();

    public SpriteTexture(Bitmap bitmap, boolean isOpaque, int count, int[] rects) {
        super(bitmap, isOpaque);

        AssertUtils.assertEquals("rects.length must be count * 4", count * 4, rects.length);
        mCount = count;
        mRects = rects;
    }

    public int getCount() {
        return mCount;
    }

    public void drawSprite(GLCanvas canvas, int index, int x, int y) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        int sourceWidth = rects[offset + 2];
        int sourceHeight = rects[offset + 3];
        mTempSource.set(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight);
        mTempTarget.set(x, y, x + sourceWidth, y + sourceHeight);
        draw(canvas, mTempSource, mTempTarget);
    }

    public void drawSprite(GLCanvas canvas, int index, int x, int y, int width, int height) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        mTempSource.set(sourceX, sourceY, sourceX + rects[offset + 2], sourceY + rects[offset + 3]);
        mTempTarget.set(x, y, x + width, y + height);
        draw(canvas, mTempSource, mTempTarget);
    }

    public void drawSpriteMixed(GLCanvas canvas, int index, int color, float ratio, int x, int y) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        int sourceWidth = rects[offset + 2];
        int sourceHeight = rects[offset + 3];
        mTempSource.set(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight);
        mTempTarget.set(x, y, x + sourceWidth, y + sourceHeight);
        drawMixed(canvas, color, ratio, mTempSource, mTempTarget);
    }

    public void drawSpriteMixed(GLCanvas canvas, int index, int color, float ratio,
                                int x, int y, int width, int height) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        mTempSource.set(sourceX, sourceY, sourceX + rects[offset + 2],
                sourceY + rects[offset + 3]);
        mTempTarget.set(x, y, x + width, y + height);
        drawMixed(canvas, color, ratio, mTempSource, mTempTarget);
    }
}
