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
package com.hippo.ehviewer.ui.legacy

import android.view.View
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.math.MathUtils

class WindowInsetsAnimationHelper(dispatchMode: Int, vararg views: View) :
    WindowInsetsAnimationCompat.Callback(dispatchMode) {
    private val views: Array<View>
    private val startPaddings = HashMap<View, Int>()
    private val endPaddings = HashMap<View, Int>()
    var animation: WindowInsetsAnimationCompat? = null

    init {
        this.views = arrayOf(*views)
    }

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        super.onPrepare(animation)
        this.animation = animation
        for (view in views) {
            startPaddings[view] = view.paddingBottom
        }
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: BoundsCompat,
    ): BoundsCompat {
        this.animation = animation
        for (view in views) {
            endPaddings[view] = view.paddingBottom
            val startPadding = if (startPaddings.containsKey(view)) startPaddings[view]!! else 0
            view.translationY = -(startPadding - view.paddingBottom).toFloat()
        }
        return bounds
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        if (animation == null) {
            return insets
        }
        var imeAnimation: WindowInsetsAnimationCompat? = null
        for (animation in runningAnimations) {
            if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                imeAnimation = animation
                break
            }
        }
        if (imeAnimation != null) {
            for (view in views) {
                val startPadding = if (startPaddings.containsKey(view)) startPaddings[view]!! else 0
                val endPadding = if (endPaddings.containsKey(view)) endPaddings[view]!! else 0
                view.translationY = MathUtils.lerp(
                    (endPadding - startPadding).toFloat(),
                    0f,
                    animation!!.interpolatedFraction,
                )
            }
        }
        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        super.onEnd(animation)
        startPaddings.clear()
        endPaddings.clear()
        this.animation = null
        for (view in views) {
            view.translationY = 0f
        }
    }
}
