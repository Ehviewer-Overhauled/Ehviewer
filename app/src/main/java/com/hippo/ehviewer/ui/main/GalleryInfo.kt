package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.ElevatedCard
import com.hippo.ehviewer.ui.tools.GalleryListCardRating

@Composable
fun GalleryInfoListItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
    isInFavScene: Boolean = false,
    showPages: Boolean = false,
) {
    CrystalCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Row {
            Card {
                EhAsyncCropThumb(
                    key = info,
                    modifier = Modifier.aspectRatio(0.6666667F).fillMaxSize(),
                )
            }
            val showFav = info.favoriteSlot != -2 && !isInFavScene
            ConstraintLayout(modifier = Modifier.padding(8.dp, 4.dp).fillMaxSize()) {
                val (titleRef, uploaderRef, ratingRef, categoryRef, postedRef, favRef, iconsRef) = createRefs()
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.constrainAs(titleRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    if (!isInFavScene) {
                        Text(
                            text = info.uploader ?: if (info.disowned) "(DISOWNED)" else "",
                            modifier = Modifier.constrainAs(uploaderRef) {
                                start.linkTo(parent.start)
                                bottom.linkTo(ratingRef.top)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GalleryListCardRating(
                        rating = info.rating,
                        modifier = Modifier.constrainAs(ratingRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(categoryRef.top)
                        },
                    )
                    val categoryColor = EhUtils.getCategoryColor(info.category)
                    val categoryText = EhUtils.getCategory(info.category).uppercase()
                    Text(
                        text = categoryText,
                        modifier = Modifier.constrainAs(categoryRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                        }.background(Color(categoryColor)).padding(vertical = 2.dp, horizontal = 8.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.constrainAs(iconsRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(if (showFav) favRef.top else postedRef.top)
                        },
                    ) {
                        Deferred({ DownloadManager.containDownloadInfo(info.gid) }) {
                            if (it) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Text(text = info.simpleLanguage.orEmpty())
                        if (info.pages != 0 && showPages) {
                            Text(text = "${info.pages}P")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.constrainAs(favRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(postedRef.top)
                        },
                    ) {
                        if (showFav) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(text = info.favoriteName.orEmpty())
                        }
                    }
                    Text(
                        text = info.posted.orEmpty(),
                        modifier = Modifier.constrainAs(postedRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryInfoGridItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
) {
    val aspect = (info.thumbWidth.toFloat() / info.thumbHeight).coerceIn(0.33F, 1.5F).takeUnless { it.isNaN() } ?: 1F
    val color = EhUtils.getCategoryColor(info.category)
    val simpleLang = info.simpleLanguage
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Box {
            EhAsyncThumb(
                model = info,
                modifier = Modifier.aspectRatio(aspect).fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
            simpleLang?.let {
                val container = Color(color)
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd).width(32.dp).height(24.dp),
                    containerColor = container,
                    contentColor = contentColorFor(container),
                ) {
                    Text(text = it.uppercase())
                }
            }
        }
    }
}
