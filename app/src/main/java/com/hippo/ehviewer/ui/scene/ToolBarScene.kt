package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

interface ToolBarScene {
    fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View

    fun onNavigationClick()

    fun getMenuResId(): Int

    fun onMenuItemClick(item: MenuItem): Boolean

    fun setLiftOnScrollTargetView(view: View?)
}
