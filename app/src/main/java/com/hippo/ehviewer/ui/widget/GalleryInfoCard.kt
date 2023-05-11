package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
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

@Composable
fun ListInfoCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
    modifier: Modifier = Modifier,
    isInFavScene: Boolean = false,
) {
    CrystalCard(modifier = Modifier.padding(6.dp)) {
        Row(
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        ) {
            Card {
                EhAsyncThumb(
                    model = info.thumb,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.aspectRatio(0.6666667F),
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
                            Text(
                                text = info.uploader ?: "(DISOWNED)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            }
                            Text(text = info.posted.orEmpty())
                        }
                    }
                }
            }
        }
    }
}
