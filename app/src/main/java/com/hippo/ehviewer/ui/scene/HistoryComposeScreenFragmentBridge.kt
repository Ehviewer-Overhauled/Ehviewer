package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import com.hippo.ehviewer.ui.scene.history.HistoryScreen

class HistoryComposeScreenFragmentBridge : BaseScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HistoryScreen {
                    toggleDrawer(GravityCompat.START)
                }
            }
        }
    }
}