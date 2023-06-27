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

import android.annotation.SuppressLint
import android.content.res.Resources.Theme
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.hippo.ehviewer.ui.MainActivity
import rikka.core.res.resolveDrawable

abstract class BaseScene : Fragment() {
    private var drawerView: View? = null
    private var drawerViewState: SparseArray<Parcelable?>? = null
    fun addAboveSnackView(view: View) {
        val activity = activity
        if (activity is MainActivity) {
            activity.addAboveSnackView(view)
        }
    }

    fun removeAboveSnackView(view: View) {
        val activity = activity
        if (activity is MainActivity) {
            activity.removeAboveSnackView(view)
        }
    }

    fun setDrawerLockMode(lockMode: Int, edgeGravity: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.setDrawerLockMode(lockMode, edgeGravity)
        }
    }

    fun getDrawerLockMode(edgeGravity: Int): Int? {
        val activity = activity
        return if (activity is MainActivity) {
            activity.getDrawerLockMode(edgeGravity)
        } else {
            null
        }
    }

    fun isDrawerOpen(drawerGravity: Int): Boolean {
        val activity = activity
        return activity is MainActivity && activity.isDrawerOpen(drawerGravity)
    }

    fun openDrawer(drawerGravity: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.openDrawer(drawerGravity)
        }
    }

    fun closeDrawer(drawerGravity: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.closeDrawer(drawerGravity)
        }
    }

    fun toggleDrawer(drawerGravity: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.toggleDrawer(drawerGravity)
        }
    }

    fun showTip(message: CharSequence?, length: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.showTip(message!!, length)
        }
    }

    fun showTip(@StringRes id: Int, length: Int) {
        val activity = activity
        if (activity is MainActivity) {
            activity.showTip(id, length)
        }
    }

    open val showLeftDrawer = true

    fun recreateDrawerView() {
        val activity = mainActivity
        activity?.createDrawerView(this)
    }

    fun createDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        drawerView = onCreateDrawerView(inflater, container, savedInstanceState)
        if (drawerView != null) {
            var saved = drawerViewState
            if (saved == null && savedInstanceState != null) {
                saved = savedInstanceState.getSparseParcelableArray(KEY_DRAWER_VIEW_STATE)
            }
            if (saved != null) {
                drawerView!!.restoreHierarchyState(saved)
            }
        }
        return drawerView
    }

    open fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return null
    }

    private fun destroyDrawerView() {
        if (drawerView != null) {
            drawerViewState = SparseArray()
            drawerView!!.saveHierarchyState(drawerViewState)
        }
        onDestroyDrawerView()
        drawerView = null
    }

    open fun onDestroyDrawerView() {}

    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = requireActivity().theme.resolveDrawable(android.R.attr.windowBackground)

        // Update left drawer locked state
        if (showLeftDrawer) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        }
        hideSoftInput()
        mainActivity!!.createDrawerView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyDrawerView()
    }

    val mainActivity: MainActivity?
        get() {
            val activity = activity
            return if (activity is MainActivity) {
                activity
            } else {
                null
            }
        }

    fun hideSoftInput() = activity?.window?.decorView?.run { SoftwareKeyboardControllerCompat(this) }?.hide()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (drawerView != null) {
            drawerViewState = SparseArray()
            drawerView!!.saveHierarchyState(drawerViewState)
            outState.putSparseParcelableArray(KEY_DRAWER_VIEW_STATE, drawerViewState)
        }
    }

    val theme: Theme
        get() = requireActivity().theme

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
        const val KEY_DRAWER_VIEW_STATE = "com.hippo.ehviewer.ui.scene.BaseScene:DRAWER_VIEW_STATE"
    }
}
