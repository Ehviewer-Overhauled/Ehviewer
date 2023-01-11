package com.hippo.ehviewer.ui.compose

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.parser.ProfileParser

suspend fun getProfile() {
    runCatching {
        (EhClient.execute(EhClient.METHOD_GET_PROFILE) as ProfileParser.Result).run {
            Settings.putDisplayName(displayName)
            Settings.putAvatar(avatar)
        }
    }
}
