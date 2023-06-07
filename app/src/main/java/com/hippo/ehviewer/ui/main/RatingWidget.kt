package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import kotlin.math.roundToInt

private val colorYellow800 = Color(0xfff9a825)

@Composable
fun MetaRatingWidget(rating: Float, ratingSize: Dp, ratingInterval: Dp) {
    val r = (rating * 2).roundToInt().coerceIn(0, 10)
    val fullStar = r / 2
    val halfStar = r % 2
    val outlineStar = 5 - fullStar - halfStar
    Row {
        repeat(fullStar) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(ratingSize),
                tint = colorYellow800,
            )
            Spacer(modifier = Modifier.width(ratingInterval))
        }
        repeat(halfStar) {
            Icon(
                imageVector = Icons.Default.StarHalf,
                contentDescription = null,
                modifier = Modifier.size(ratingSize),
                tint = colorYellow800,
            )
            Spacer(modifier = Modifier.width(ratingInterval))
        }
        repeat(outlineStar) {
            Icon(
                imageVector = Icons.Default.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(ratingSize),
                tint = colorYellow800,
            )
            Spacer(modifier = Modifier.width(ratingInterval))
        }
    }
}

@Composable
fun GalleryListCardRating(rating: Float) {
    MetaRatingWidget(
        rating = rating,
        ratingSize = dimensionResource(id = R.dimen.rating_size),
        ratingInterval = dimensionResource(id = R.dimen.rating_interval),
    )
}

@Composable
fun GalleryDetailRating(rating: Float) {
    MetaRatingWidget(rating = rating, ratingSize = 48.dp, ratingInterval = 12.dp)
}
