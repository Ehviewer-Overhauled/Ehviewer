package com.hippo.widget

import android.content.Context
import android.util.AttributeSet
import com.hippo.ehviewer.R

class ContentLayout2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ContentLayout(context, attrs) {
    override fun getLayout(): Int {
        return R.layout.widget_content_layout_2
    }
}