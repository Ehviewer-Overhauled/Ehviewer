package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

@Composable
private fun crystalCardBorder(): BorderStroke {
    val originBorder = CardDefaults.outlinedCardBorder()
    return remember { originBorder.copy(brush = SolidColor(Color.Transparent)) }
}

@Composable
fun CrystalCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    onClick = onClick,
    onLongClick = onLongClick,
    modifier = modifier,
    border = crystalCardBorder(),
    content = content,
)

@Composable
fun CrystalCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    onClick = onClick,
    modifier = modifier,
    border = crystalCardBorder(),
    content = content,
)

@Composable
fun CrystalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    modifier = modifier,
    border = crystalCardBorder(),
    content = content,
)
