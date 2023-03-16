package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo

@Composable
private fun GalleryDetailHeaderInfoCard(
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

@Composable
fun GalleryDetailHeaderCard(
    galleryInfo: GalleryInfo,
    galleryDetail: GalleryDetail?,
    onInfoCardClick: () -> Unit,
    onUploaderChipClick: () -> Unit,
    onCategoryChipClick: () -> Unit,
    modifier: Modifier
) {
    ElevatedCard(
        modifier = modifier
    ) {
        Row {
            Card {
                EhAsyncThumb(
                    model = galleryInfo.thumb,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(dimensionResource(id = R.dimen.gallery_detail_thumb_height))
                        .width(dimensionResource(id = R.dimen.gallery_detail_thumb_width))
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            Column(
                modifier = Modifier.height(dimensionResource(id = R.dimen.gallery_detail_thumb_height)),
                horizontalAlignment = Alignment.End
            ) {
                galleryDetail?.let {
                    GalleryDetailHeaderInfoCard(
                        galleryDetail = it,
                        onClick = onInfoCardClick,
                        modifier = Modifier.padding(
                            top = 8.dp,
                            end = dimensionResource(id = R.dimen.keyline_margin)
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1F))
                val categoryText =
                    EhUtils.getCategory(galleryInfo.category).toUpperCase(Locale.current)
                AssistChip(
                    onClick = onCategoryChipClick,
                    label = { Text(text = categoryText, maxLines = 1) },
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null
                        )
                    })

                val uploaderText = galleryInfo.uploader.orEmpty().toUpperCase(Locale.current)
                AssistChip(
                    onClick = onUploaderChipClick,
                    label = { Text(text = uploaderText, maxLines = 1) },
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                    })
            }
        }
    }
}
