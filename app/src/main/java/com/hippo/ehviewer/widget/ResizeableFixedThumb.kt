package com.hippo.ehviewer.widget

import android.content.Context
import android.util.AttributeSet
import com.hippo.ehviewer.Settings

class ResizeableFixedThumb @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FixedThumb(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(Settings.getListThumbSize() * 2, Settings.getListThumbSize() * 3)
    }
}