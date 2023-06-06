package com.hippo.ehviewer.ui.login

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun postLogin() = coroutineScope {
    launch {
        runCatching {
            EhEngine.getProfile().run {
                Settings.displayName = displayName
                Settings.avatar = avatar
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
    runCatching {
        // For the `star` cookie
        EhEngine.getNews(false)
        EhCookieStore.copyCookie(EhUrl.DOMAIN_E, EhUrl.DOMAIN_EX, EhCookieStore.KEY_STAR)

        // Sad panda check
        Settings.gallerySite = EhUrl.SITE_EX
        EhEngine.getUConfig()
    }.onFailure {
        Settings.gallerySite = EhUrl.SITE_E
        launch {
            runCatching {
                EhEngine.getUConfig()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }.isSuccess
}
