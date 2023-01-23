package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.scene.history.downloadManager
import eu.kanade.tachiyomi.util.system.pxToDp
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListInfoCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
) {
    OutlinedCard(
        modifier = Modifier.padding(6.dp),
        border = remember { BorderStroke(1.dp, Color.Transparent) }
    ) {
        val listCardSize = remember { Settings.getListThumbSize().pxToDp }
        Row(
            modifier = Modifier
                .height((listCardSize * 3).dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
        ) {
            Card {
                AsyncImage(
                    model = info.thumb,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width((listCardSize * 2).dp)
                        .height((listCardSize * 3).dp)
                )
            }
            Column(Modifier.padding(8.dp, 4.dp)) {
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(
                            text = info.uploader ?: "(DISOWNED)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                        SimpleRatingWidget(rating = info.rating)
                        Text(
                            text = EhUtils.getCategory(info.category).uppercase(Locale.ROOT),
                            modifier = Modifier
                                .background(
                                    Color(
                                        EhUtils.getCategoryColor(
                                            info.category
                                        )
                                    )
                                )
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (remember { downloadManager.containDownloadInfo(info.gid) }) {
                                Icon(
                                    painterResource(id = R.drawable.v_download_x16),
                                    contentDescription = null
                                )
                            }
                            if (info.favoriteSlot != -2) {
                                Icon(
                                    painterResource(id = R.drawable.v_heart_x16),
                                    contentDescription = null
                                )
                            }
                            Text(
                                text = info.simpleLanguage.orEmpty(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = info.posted.orEmpty(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

