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
        EhCookieStore.copyNecessaryCookies()

        // Get cookies for image limits
        launch {
            runCatching {
                EhEngine.getUConfig(EhUrl.URL_UCONFIG_E)
            }.onFailure {
                it.printStackTrace()
            }
        }

        // Sad panda check
        Settings.gallerySite = EhUrl.SITE_EX
        EhEngine.getUConfig()
    }.onFailure {
        Settings.gallerySite = EhUrl.SITE_E
    }.isSuccess
}
