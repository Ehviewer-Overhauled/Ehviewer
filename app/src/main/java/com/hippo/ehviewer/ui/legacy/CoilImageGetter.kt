package com.hippo.ehviewer.ui.legacy

import android.graphics.drawable.DrawableWrapper
import android.text.Html
import android.widget.TextView
import coil.imageLoader
import com.hippo.ehviewer.ktbuilder.imageRequest

class CoilImageGetter(
    private val textView: TextView,
) : Html.ImageGetter {
    override fun getDrawable(source: String) = object : DrawableWrapper(null) {}.apply {
        textView.context.imageLoader.enqueue(
            textView.context.imageRequest {
                data(source)
                crossfade(false)
                target { drawable ->
                    setDrawable(drawable)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                    textView.text = textView.text
                }
            },
        )
    }
}
