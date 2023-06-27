package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.MainThread
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
    GalleryListUrlParser.parse(url)?.let { lub ->
        Bundle().apply {
            putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_LIST_URL_BUILDER)
            putParcelable(GalleryListScene.KEY_LIST_URL_BUILDER, lub)
            navAnimated(R.id.galleryListScene, this)
        }
        return true
    }

    GalleryDetailUrlParser.parse(url)?.apply {
        Bundle().apply {
            putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
            putLong(GalleryDetailScene.KEY_GID, gid)
            putString(GalleryDetailScene.KEY_TOKEN, token)
            navAnimated(R.id.galleryDetailScene, this)
        }
        return true
    }

    GalleryPageUrlParser.parse(url)?.apply {
        Bundle().apply {
            putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
            putLong(ProgressScene.KEY_GID, gid)
            putString(ProgressScene.KEY_PTOKEN, pToken)
            putInt(ProgressScene.KEY_PAGE, page)
            navAnimated(R.id.progressScene, this)
        }
        return true
    }
    return false
}

@MainThread
fun Fragment.navAnimated(id: Int, args: Bundle?, singleTop: Boolean = false) = findNavController().navAnimated(id, args, singleTop)
