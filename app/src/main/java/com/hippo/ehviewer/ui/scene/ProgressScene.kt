package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching

class ProgressScene : BaseScene() {
    override val showLeftDrawer = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                val action = rememberSaveable { requireArguments().getString(KEY_ACTION, INVALID) }
                val gid = rememberSaveable { requireArguments().getLong(KEY_GID, -1) }
                val token = rememberSaveable { requireArguments().getString(KEY_PTOKEN, INVALID) }
                val page = rememberSaveable { requireArguments().getInt(KEY_PAGE, -1) }
                val wrong = stringResource(id = R.string.error_something_wrong_happened)
                var error by rememberSaveable { mutableStateOf("") }
                LaunchedEffect(error) {
                    if (error.isEmpty()) {
                        if (action != ACTION_GALLERY_TOKEN || gid == -1L || token == INVALID || page == -1) {
                            error = wrong
                        } else {
                            runSuspendCatching {
                                EhEngine.getGalleryToken(gid, token, page)
                            }.onSuccess {
                                withUIContext {
                                    findNavController().popBackStack()
                                    navAnimated(
                                        R.id.galleryDetailScene,
                                        bundleOf(
                                            GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GID_TOKEN,
                                            GalleryDetailScene.KEY_GID to gid,
                                            GalleryDetailScene.KEY_TOKEN to it,
                                            GalleryDetailScene.KEY_PAGE to page,
                                        ),
                                    )
                                }
                            }.onFailure {
                                error = ExceptionUtils.getReadableString(it)
                            }
                        }
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    if (error.isNotBlank()) {
                        Column(
                            modifier = Modifier.clickable { error = "" },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.big_sad_pandroid),
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                            )
                            Text(
                                text = wrong,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_GALLERY_TOKEN = "gallery_token"
        const val KEY_GID = "gid"
        const val KEY_PTOKEN = "ptoken"
        const val KEY_PAGE = "page"
        const val INVALID = "invalid"
    }
}
