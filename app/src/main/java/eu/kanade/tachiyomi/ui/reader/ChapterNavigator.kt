package eu.kanade.tachiyomi.ui.reader

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.isTabletUi
import kotlin.math.roundToInt

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
) {
    val isTabletUi = LocalConfiguration.current.isTabletUi()
    val horizontalPadding = if (isTabletUi) 24.dp else 16.dp
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val view = LocalView.current

    // We explicitly handle direction based on the reader viewer rather than the system direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Match with toolbar background color set in ReaderActivity
            val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
            if (totalPages > 1) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(backgroundColor).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = currentPage.toString())

                        val interactionSource = remember { MutableInteractionSource() }
                        val sliderDragged by interactionSource.collectIsDraggedAsState()
                        LaunchedEffect(currentPage) {
                            if (sliderDragged) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                        Slider(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            value = currentPage.toFloat(),
                            valueRange = 1f..totalPages.toFloat(),
                            steps = totalPages - 2,
                            onValueChange = {
                                onSliderValueChange(it.roundToInt() - 1)
                            },
                            interactionSource = interactionSource,
                        )

                        Text(text = totalPages.toString())
                    }
                }
            }
        }
    }
}
