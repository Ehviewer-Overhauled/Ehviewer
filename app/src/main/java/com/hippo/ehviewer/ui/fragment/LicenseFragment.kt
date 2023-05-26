package com.hippo.ehviewer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.compose.setMD3Content
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer

class LicenseFragment : BaseFragment() {
    override fun getFragmentTitle(): Int {
        return R.string.license
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                LibrariesContainer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
