package com.hippo.ehviewer.ui.main.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.main.CrystalCard
import com.hippo.ehviewer.ui.main.ElevatedCard
import com.hippo.ehviewer.ui.main.GalleryListCardRating

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
            Column(modifier = Modifier.padding(8.dp, 4.dp)) {
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.weight(1f))
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column {
                            if (!isInFavScene) {
                                Text(
                                    text = info.uploader ?: "(DISOWNED)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            GalleryListCardRating(rating = info.rating)
                            val categoryColor = EhUtils.getCategoryColor(info.category)
                            val categoryText = EhUtils.getCategory(info.category).uppercase()
                            Text(
                                text = categoryText,
                                modifier = Modifier.background(Color(categoryColor)).padding(vertical = 2.dp, horizontal = 8.dp),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (DownloadManager.containDownloadInfo(info.gid)) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                if (info.favoriteSlot != -2 && !isInFavScene) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(text = info.simpleLanguage.orEmpty())
                                if (info.pages != 0 && showPages) {
                                    Text(text = "${info.pages}P")
                                }
                            }
                            Text(text = info.posted.orEmpty())
                        }
                    }
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
            val container = Color(color)
            Badge(
                modifier = Modifier.align(Alignment.TopEnd).width(32.dp).height(24.dp),
                containerColor = container,
                contentColor = contentColorFor(container),
            ) {
                Text(text = simpleLang?.uppercase().orEmpty())
            }
        }
    }
}
