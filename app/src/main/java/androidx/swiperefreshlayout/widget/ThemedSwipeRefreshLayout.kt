package androidx.swiperefreshlayout.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import androidx.core.graphics.ColorUtils
import rikka.core.res.resolveColor


@SuppressLint("PrivateResource")
class ThemedSwipeRefreshLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    init {
        val surfaceColor =
            context.theme.resolveColor(com.google.android.material.R.attr.colorSurface)
        val overlayColor =
            context.getColor(com.google.android.material.R.color.m3_popupmenu_overlay_color)
        val backgroundColor = ColorUtils.compositeColors(overlayColor, surfaceColor)
        (mCircleView.background as ShapeDrawable).paint.color = backgroundColor
        setColorSchemeColors(context.theme.resolveColor(com.google.android.material.R.attr.colorAccent))
    }
}