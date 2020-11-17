/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.easyrecyclerview;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private int mMargin;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    public MarginItemDecoration(int margin, int paddingLeft, int paddingTop,
                                int paddingRight, int paddingBottom) {
        setMargin(margin, paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        outRect.set(mMargin, mMargin, mMargin, mMargin);
    }

    /**
     * @param margin        gap between two item
     * @param paddingLeft   gap between RecyclerView left and left item left
     * @param paddingTop    gap between RecyclerView top and top item top
     * @param paddingRight  gap between RecyclerView right and right item right
     * @param paddingBottom gap between RecyclerView bottom and bottom item bottom
     */
    public void setMargin(int margin, int paddingLeft, int paddingTop,
                          int paddingRight, int paddingBottom) {
        int halfMargin = margin / 2;
        mMargin = halfMargin;
        mPaddingLeft = paddingLeft - halfMargin;
        mPaddingTop = paddingTop - halfMargin;
        mPaddingRight = paddingRight - halfMargin;
        mPaddingBottom = paddingBottom - halfMargin;
    }

    public void applyPaddings(View view) {
        view.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
    }
}
