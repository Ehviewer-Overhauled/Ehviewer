package com.hippo.app

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import com.google.android.material.slider.Slider

class HapticSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : Slider(context, attrs) {
    init {
        addOnChangeListener { _, _, _ -> performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
}