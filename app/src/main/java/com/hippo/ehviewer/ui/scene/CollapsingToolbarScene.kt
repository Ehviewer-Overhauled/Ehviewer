package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.hippo.ehviewer.databinding.SceneCollapsingToolbarBinding

abstract class CollapsingToolbarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneCollapsingToolbarBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneCollapsingToolbarBinding.inflate(inflater, container, false)
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
        binding.appbar.setExpanded(false, true)
    }

    override fun onNavigationClick() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    fun setNavigationIcon(@DrawableRes resId: Int) {
        binding.toolbar.setNavigationIcon(resId)
    }

    override fun setLiftOnScrollTargetView(view: View?) {
        binding.appbar.setLiftOnScrollTargetView(view)
    }

    fun setTitle(title: CharSequence?) {
        binding.toolbar.title = title
    }
}