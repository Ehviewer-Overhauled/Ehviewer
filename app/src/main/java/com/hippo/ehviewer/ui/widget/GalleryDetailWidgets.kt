package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryDetail

@Composable
fun GalleryDetailHeaderInfoCard(
    galleryDetail: GalleryDetail,
    onClick: () -> Unit,
    modifier: Modifier
) {
    galleryDetail.run {
        Card(
            onClick = onClick,
            modifier = modifier
        ) {
            ConstraintLayout(
                modifier = Modifier.padding(8.dp)
            ) {
                val (langRef, sizeRef, favRef, pagesRef, postRef) = createRefs()

                Text(
                    text = language.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.constrainAs(langRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                )
                Text(
                    text = size.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .constrainAs(sizeRef) {
                            baseline.linkTo(langRef.baseline)
                            end.linkTo(parent.end)
                            horizontalBias = 1.0f
                            start.linkTo(langRef.end)
                        }
                        .padding(start = 16.dp)
                )
                Text(
                    text = stringResource(
                        id = R.string.favored_times,
                        favoriteCount
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .constrainAs(favRef) {
                            start.linkTo(parent.start)
                            top.linkTo(langRef.bottom)
                        }
                        .padding(top = 8.dp)
                )
                Text(
                    text = pluralStringResource(
                        id = R.plurals.page_count,
                        pages,
                        pages
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .constrainAs(pagesRef) {
                            baseline.linkTo(favRef.baseline)
                            end.linkTo(parent.end)
                            horizontalBias = 1.0f
                            start.linkTo(favRef.end)
                        }
                        .padding(start = 16.dp)
                )
                Text(
                    text = posted.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .constrainAs(postRef) {
                            end.linkTo(parent.end)
                            start.linkTo(parent.start)
                            top.linkTo(favRef.bottom)
                        }
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
