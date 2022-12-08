package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.ehviewer.R

abstract class CollapsingToolbarScene : ToolbarScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.scene_collapsing_toolbar, container, false) as ViewGroup
        mToolbar = view.findViewById(R.id.toolbar)
        mAppBarLayout = view.findViewById(R.id.appbar)
        val contentView = onCreateViewWithToolbar(inflater, view, savedInstanceState)
        return view.apply { addView(contentView, 0) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAppBarLayout?.setExpanded(false, true)
    }
}