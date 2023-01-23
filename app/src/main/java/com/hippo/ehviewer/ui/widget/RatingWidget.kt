package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.hippo.ehviewer.R

@Composable
fun SimpleRatingWidget(rating: Float) {
    val r = (rating * 2).toInt().coerceAtLeast(0).coerceAtMost(10)
    val fullStar = r.floorDiv(2)
    val halfStar = r % 2
    val outlineStar = 5 - fullStar - halfStar
    val colorYellow800 = Color(0xfff9a825)
    Row {
        repeat(fullStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
        repeat(halfStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_half_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
        repeat(outlineStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_outline_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
    }
}
