package com.hippo.ehviewer.ui.widget

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hippo.ehviewer.Settings
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
    val localFavName = stringResource(id = R.string.local_favorites)
    val addToFavSuccess = stringResource(id = R.string.add_to_favorite_success)
    val addToFavFailed = stringResource(id = R.string.add_to_favorite_failure)

    suspend fun addToFav(slot: Int) {
        val favNameList = Settings.getFavCat().toMutableList().apply { add(0, localFavName) }
        runCatching {
            if (slot == -1) {
                EhDB.putLocalFavorites(info)
            } else {
                EhEngine.addFavorites(
                    info.gid,
                    info.token,
                    slot,
                    ""
                )
                info.favoriteSlot = slot
                info.favoriteName = favNameList[slot + 1]
                EhDB.putHistoryInfoNonRefresh(info)
            }
        }.onSuccess {
            showTip(addToFavSuccess)
        }.onFailure {
            showTip(addToFavFailed)
        }
        onDismissRequest()
        dialogStatus = DialogStatus.PRIMARY
    }

    when (dialogStatus) {
        DialogStatus.PRIMARY -> {
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
                                onDismissRequest()
                                dialogStatus = DialogStatus.PRIMARY
                            },
                            icon = painterResource(id = R.drawable.v_book_open_x24),
                            text = stringResource(id = R.string.read)
                        )
                        if (remember { downloadManager.containDownloadInfo(info.gid) }) {
                            DialogSelectorItem(
                                onClick = {
                                    dialogStatus = DialogStatus.CONFIRM_REMOVE_DOWNLOAD
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
                                    val defaultFav = Settings.getDefaultFavSlot()
                                    if (defaultFav in -1..9) {
                                        launchIO {
                                            addToFav(defaultFav)
                                        }
                                    } else {
                                        dialogStatus = DialogStatus.SELECT_FAVOURITE
                                    }
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

        DialogStatus.CONFIRM_REMOVE_DOWNLOAD -> {
            AlertDialog(
                onDismissRequest = {
                    onDismissRequest()
                    dialogStatus = DialogStatus.PRIMARY
                },
                title = {
                    Text(text = stringResource(id = R.string.download_remove_dialog_title))
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.download_remove_dialog_message,
                            info.title.orEmpty()
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissRequest()
                        dialogStatus = DialogStatus.PRIMARY
                        downloadManager.deleteDownload(info.gid)
                    }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            )
        }

        DialogStatus.SELECT_FAVOURITE -> {
            SelectFavouriteDialog(
                onFolderSelected = {
                    launchIO {
                        addToFav(it)
                    }
                },
                onDismissRequest = {
                    onDismissRequest()
                    dialogStatus = DialogStatus.PRIMARY
                }
            )
        }
    }
}

@Composable
fun SelectFavouriteDialog(
    onFolderSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var rememberFavSlot by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = rememberFavSlot, onCheckedChange = { rememberFavSlot = it })
                Text(text = stringResource(id = R.string.remember_favorite_collection))
            }
        },
        title = { Text(text = stringResource(id = R.string.add_favorites_dialog_title)) },
        text = {
            val localFavName = stringResource(id = R.string.local_favorites)
            Column {
                remember {
                    Settings.getFavCat().toMutableList().apply { add(0, localFavName) }
                }.forEachIndexed { index, s ->
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            Settings.putDefaultFavSlot(if (rememberFavSlot) index - 1 else Settings.INVALID_DEFAULT_FAV_SLOT)
                            onFolderSelected(index - 1)
                        },
                        Modifier.fillMaxWidth()
                    ) {
                        Text(text = s)
                    }
                }
            }
        }
    )
}

private enum class DialogStatus {
    PRIMARY,
    CONFIRM_REMOVE_DOWNLOAD,
    SELECT_FAVOURITE
}
