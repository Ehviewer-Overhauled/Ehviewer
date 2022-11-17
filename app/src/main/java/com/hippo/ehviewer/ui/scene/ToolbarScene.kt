/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.size
import com.google.android.material.appbar.AppBarLayout
import com.hippo.ehviewer.R

abstract class ToolbarScene : BaseScene() {
    protected var mToolbar: Toolbar? = null
    protected var mAppBarLayout: AppBarLayout? = null
    private var mTempTitle: CharSequence? = null

    abstract fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scene_toolbar, container, false) as ViewGroup
        mToolbar = view.findViewById(R.id.toolbar)
        mAppBarLayout = view.findViewById(R.id.appbar)
        val contentView = onCreateViewWithToolbar(inflater, view, savedInstanceState)
        return view.apply { addView(contentView, 0) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mToolbar = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mToolbar?.apply {
            mTempTitle?.let { title = it }
            val menuResId = getMenuResId()
            if (menuResId != 0) {
                inflateMenu(menuResId)
                setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
            }
            setNavigationOnClickListener { onNavigationClick() }
        }
    }

    fun hideMenu() {
        mToolbar?.menu?.clear()
    }

    fun showMenu(menuResId: Int) {
        if (mToolbar?.menu?.size != 0) return
        mToolbar?.apply {
            inflateMenu(menuResId)
            setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
        }
    }

    open fun getMenuResId(): Int {
        return 0
    }

    open fun onMenuItemClick(item: MenuItem): Boolean {
        return false
    }

    open fun onNavigationClick() {}

    fun setNavigationIcon(@DrawableRes resId: Int) {
        mToolbar?.setNavigationIcon(resId)
    }

    fun setTitle(@StringRes resId: Int) {
        setTitle(getString(resId))
    }

    fun setTitle(title: CharSequence?) {
        mToolbar?.title = title
        mTempTitle = title
    }

    fun setLiftOnScrollTargetView(view: View?) {
        mAppBarLayout?.setLiftOnScrollTargetView(view)
    }
}