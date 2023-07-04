package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FullCircle: Float = (PI * 2).toFloat()
private const val QuarterCircle = PI / 2

@Composable
fun CircularLayout(
    modifier: Modifier = Modifier,
    radius: Dp,
    placeFirstItemInCenter: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val radiusPx = radius.toPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable -> measurable.measure(itemConstraints) }
        val outPlaceables = if (placeFirstItemInCenter) placeables.drop(1) else placeables
        val theta = FullCircle / (outPlaceables.count())

        layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
        ) {
            if (placeFirstItemInCenter) {
                placeables[0].let {
                    val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                    val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                    it.place(
                        x = centerOffsetX,
                        y = centerOffsetY,
                    )
                }
            }
            outPlaceables.forEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
                it.place(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt(),
                )
            }
        }
    }
}
