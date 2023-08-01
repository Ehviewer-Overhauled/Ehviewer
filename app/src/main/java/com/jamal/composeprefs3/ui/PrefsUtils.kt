package com.jamal.composeprefs3.ui

import androidx.compose.runtime.Composable
import java.math.BigDecimal
import java.math.RoundingMode

fun Any?.ifNotNullThen(content: @Composable () -> Unit): (@Composable () -> Unit)? =
    if (this != null) content else null

fun Boolean.ifTrueThen(content: @Composable () -> Unit): (@Composable () -> Unit)? =
    if (this) content else null

fun roundToDP(value: Double, places: Int): Double {
    return BigDecimal(value).setScale(places, RoundingMode.HALF_EVEN).toDouble()
}

fun roundToDP(value: Float, places: Int): Float {
    return BigDecimal(value.toDouble()).setScale(places, RoundingMode.HALF_EVEN).toFloat()
}
