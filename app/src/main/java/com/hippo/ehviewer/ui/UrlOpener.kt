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
package com.hippo.ehviewer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.NavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.ProgressScene
import eu.kanade.tachiyomi.ui.reader.ReaderActivity

object UrlOpener {
    fun openUrl(context: Context, url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        val uri = Uri.parse(url)
        val customTabsIntent = CustomTabsIntent.Builder()
        customTabsIntent.setShowTitle(true)
        try {
            customTabsIntent.build().launchUrl(context, uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_browser_installed, Toast.LENGTH_LONG).show()
        }
    }
}

@MainThread
fun Context.jumpToReaderByPage(url: String, detail: GalleryDetail): Boolean {
    fun jump(page: Int) {
        Intent(this, ReaderActivity::class.java).apply {
            action = ReaderActivity.ACTION_EH
            putExtra(ReaderActivity.KEY_GALLERY_INFO, detail)
            putExtra(ReaderActivity.KEY_PAGE, page)
            startActivity(this)
        }
    }
    GalleryPageUrlParser.parse(url)?.let {
        if (it.gid == detail.gid) {
            jump(it.page)
            return true
        }
    }
    if (url.startsWith("#c")) {
        runCatching {
            jump(url.replace("#c", "").toInt() - 1)
            return true
        }
    }
    return false
}

@MainThread
fun NavController.navWithUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    GalleryListUrlParser.parse(url)?.let { lub ->
        Bundle().apply {
            putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_LIST_URL_BUILDER)
            putParcelable(GalleryListScene.KEY_LIST_URL_BUILDER, lub)
            navigate(R.id.galleryListScene, this)
        }
        return true
    }

    GalleryDetailUrlParser.parse(url)?.apply {
        Bundle().apply {
            putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
            putLong(GalleryDetailScene.KEY_GID, gid)
            putString(GalleryDetailScene.KEY_TOKEN, token)
            navigate(R.id.galleryDetailScene, this)
        }
        return true
    }

    GalleryPageUrlParser.parse(url)?.apply {
        Bundle().apply {
            putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
            putLong(ProgressScene.KEY_GID, gid)
            putString(ProgressScene.KEY_PTOKEN, pToken)
            putInt(ProgressScene.KEY_PAGE, page)
            navigate(R.id.progressScene, this)
        }
        return true
    }
    return false
}
