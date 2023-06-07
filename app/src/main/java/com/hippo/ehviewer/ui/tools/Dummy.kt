package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// This file holds widgets ** Compose Material 3 ** library SHOULD but NOT implemented yet.

@Composable
fun OutlinedCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    modifier = modifier,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        ),
        content = content,
    )
}

@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) = ElevatedCard(
    modifier = modifier,
    shape = shape,
    colors = colors,
    elevation = elevation,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        ),
        content = content,
    )
}
