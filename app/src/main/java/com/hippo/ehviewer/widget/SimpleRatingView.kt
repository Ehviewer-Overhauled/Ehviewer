package com.hippo.ehviewer.widget

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.hippo.ehviewer.ui.widget.SimpleRatingWidget

class SimpleRatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    var rating: Float = 0.0f

    @Composable
    override fun Content() {
        SimpleRatingWidget(rating)
    }
}
