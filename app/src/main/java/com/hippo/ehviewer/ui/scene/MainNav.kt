package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser

@MainThread
@SuppressLint("PrivateResource")
fun NavController.navAnimated(id: Int, args: Bundle?, singleTop: Boolean = false) {
    val options: NavOptions = NavOptions.Builder().setLaunchSingleTop(singleTop)
        .setEnterAnim(androidx.fragment.R.animator.fragment_open_enter)
        .setExitAnim(androidx.fragment.R.animator.fragment_open_exit)
        .setPopEnterAnim(androidx.fragment.R.animator.fragment_close_enter)
        .setPopExitAnim(androidx.fragment.R.animator.fragment_close_exit)
        .build()
    navigate(id, args, options)
}

@MainThread
fun NavController.navWithUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    GalleryListUrlParser.parse(url)?.let {
        navAnimated(
            R.id.galleryListScene,
            bundleOf(
                GalleryListScene.KEY_ACTION to GalleryListScene.ACTION_LIST_URL_BUILDER,
                GalleryListScene.KEY_LIST_URL_BUILDER to it,
            ),
        )
        return true
    }

    GalleryDetailUrlParser.parse(url)?.apply {
        navAnimated(
            R.id.galleryDetailScene,
            bundleOf(
                GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GID_TOKEN,
                GalleryDetailScene.KEY_GID to gid,
                GalleryDetailScene.KEY_TOKEN to token,
            ),
        )
        return true
    }

    GalleryPageUrlParser.parse(url)?.apply {
        navAnimated(
            R.id.progressScene,
            bundleOf(
                ProgressScene.KEY_ACTION to ProgressScene.ACTION_GALLERY_TOKEN,
                ProgressScene.KEY_GID to gid,
                ProgressScene.KEY_PTOKEN to pToken,
                ProgressScene.KEY_PAGE to page,
            ),
        )
        return true
    }
    return false
}

@MainThread
fun Fragment.navAnimated(id: Int, args: Bundle?, singleTop: Boolean = false) = findNavController().navAnimated(id, args, singleTop)
