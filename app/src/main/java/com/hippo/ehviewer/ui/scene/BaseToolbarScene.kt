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
import androidx.core.view.size
import com.hippo.ehviewer.databinding.SceneToolbarBinding

abstract class BaseToolbarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneToolbarBinding.inflate(inflater, container, false)
        val contentView = onCreateViewWithToolbar(inflater, binding.root, savedInstanceState)
        return binding.root.apply { addView(contentView, 0) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            val menuResId = getMenuResId()
            if (menuResId != 0) {
                inflateMenu(menuResId)
                setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
            }
            setNavigationOnClickListener { onNavigationClick() }
        }
    }

    fun showMenu(menuResId: Int) {
        if (binding.toolbar.menu?.size != 0) return
        binding.toolbar.apply {
            inflateMenu(menuResId)
            setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
        }
    }

    override fun getMenuResId(): Int {
        return 0
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return false
    }

    override fun onNavigationClick() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    fun setNavigationIcon(@DrawableRes resId: Int) {
        binding.toolbar.setNavigationIcon(resId)
    }

    fun setTitle(@StringRes resId: Int) {
        setTitle(getString(resId))
    }

    fun setTitle(title: CharSequence?) {
        binding.toolbar.title = title
    }

    override fun setLiftOnScrollTargetView(view: View?) {
        binding.appbar.setLiftOnScrollTargetView(view)
    }
}
