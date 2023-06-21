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
package com.hippo.ehviewer.ui.legacy

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlin.math.abs
import kotlin.math.ceil

abstract class SimpleSmoothScroller(context: Context, millisecondsPerInch: Float) :
    LinearSmoothScroller(context) {
    private val mMillisecondsPerPx: Float

    init {
        mMillisecondsPerPx = millisecondsPerInch / context.resources.displayMetrics.densityDpi
    }

    override fun calculateTimeForScrolling(dx: Int): Int {
        return if (mMillisecondsPerPx <= 0) {
            super.calculateTimeForScrolling(dx)
        } else {
            ceil((abs(dx) * mMillisecondsPerPx).toDouble()).toInt()
        }
    }
}
