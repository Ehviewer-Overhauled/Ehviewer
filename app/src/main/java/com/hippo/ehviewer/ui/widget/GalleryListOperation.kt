package com.hippo.ehviewer.ui.widget

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.DelicateCoroutinesApi

private val downloadManager = EhApplication.downloadManager

@Composable
private fun DialogSelectorItem(
    onClick: () -> Unit,
    icon: Painter,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun <T : GalleryInfo> GalleryListLongClickDialog(
    info: T,
    onDismissRequest: () -> Unit,
    showTip: (String) -> Unit
) {
    var dialogStatus by remember { mutableStateOf(DialogStatus.PRIMARY) }
    val context = LocalContext.current

    val removeFromFavouriteSuccess = stringResource(id = R.string.remove_from_favorite_success)
    val removeFromFavouriteFailure = stringResource(id = R.string.remove_from_favorite_failure)

    AlertDialog(
        onDismissRequest = {
            dialogStatus = DialogStatus.PRIMARY
            onDismissRequest()
        },
        title = {
            Text(
                text = EhUtils.getSuitableTitle(info),
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                DialogSelectorItem(
                    onClick = {
                        val intent = Intent(context, ReaderActivity::class.java)
                        intent.action = ReaderActivity.ACTION_EH
                        intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, info)
                        startActivity(context, intent, null)
                        dialogStatus = DialogStatus.PRIMARY
                        onDismissRequest()
                    },
                    icon = painterResource(id = R.drawable.v_book_open_x24),
                    text = stringResource(id = R.string.read)
                )
                if (remember { downloadManager.containDownloadInfo(info.gid) }) {
                    DialogSelectorItem(
                        onClick = {
                            dialogStatus = DialogStatus.PRIMARY
                        },
                        icon = painterResource(id = R.drawable.v_delete_x24),
                        text = stringResource(id = R.string.delete_downloads)
                    )
                } else {
                    DialogSelectorItem(
                        onClick = {
                            dialogStatus = DialogStatus.PRIMARY
                        },
                        icon = painterResource(id = R.drawable.v_download_x24),
                        text = stringResource(id = R.string.download)
                    )
                }
                if (info.favoriteSlot == -2) {
                    DialogSelectorItem(
                        onClick = {
                            dialogStatus = DialogStatus.PRIMARY
                        },
                        icon = painterResource(id = R.drawable.v_heart_x24),
                        text = stringResource(id = R.string.add_to_favourites)
                    )
                } else {
                    DialogSelectorItem(
                        onClick = {
                            onDismissRequest()
                            dialogStatus = DialogStatus.PRIMARY
                            launchIO {
                                runCatching {
                                    EhDB.removeLocalFavorites(info.gid)
                                    EhEngine.addFavorites(info.gid, info.token, -1, "")
                                    info.favoriteSlot = -2
                                    info.favoriteName = null
                                    EhDB.putHistoryInfoNonRefresh(info)
                                    showTip(removeFromFavouriteSuccess)
                                }.onFailure {
                                    showTip(removeFromFavouriteFailure)
                                }
                            }
                        },
                        icon = painterResource(id = R.drawable.v_heart_broken_x24),
                        text = stringResource(id = R.string.remove_from_favourites)
                    )
                }
                if (remember { downloadManager.containDownloadInfo(info.gid) }) {
                    DialogSelectorItem(
                        onClick = {
                            dialogStatus = DialogStatus.PRIMARY
                        },
                        icon = painterResource(id = R.drawable.v_folder_move_x24),
                        text = stringResource(id = R.string.download_move_dialog_title)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

private enum class DialogStatus {
    PRIMARY,
    SELECT_FAVOURITE
}
