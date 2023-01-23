package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.scene.history.HistoryScreen

class HistoryComposeScreenFragmentBridge : BaseScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Mdc3Theme {
                    HistoryScreen(this@HistoryComposeScreenFragmentBridge)
                }
            }
        }
    }

    @MainThread
    fun toggleNavigationDrawer() {
        toggleDrawer(GravityCompat.START)
    }

    @MainThread
    fun navToDetail(gi: GalleryInfo) {
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
        navigate(R.id.galleryDetailScene, args)
    }
}