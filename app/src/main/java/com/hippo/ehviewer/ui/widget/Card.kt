package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CrystalCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    onClick = onClick,
    modifier = modifier,
    border = BorderStroke(CardDefaults.outlinedCardBorder().width, Color.Transparent),
    content = content,
)

@Composable
fun CrystalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    modifier = modifier,
    border = BorderStroke(CardDefaults.outlinedCardBorder().width, Color.Transparent),
    content = content,
)
