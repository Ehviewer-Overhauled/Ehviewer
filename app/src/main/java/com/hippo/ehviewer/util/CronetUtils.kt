package com.hippo.ehviewer.util

import com.hippo.ehviewer.Settings

val isCronetSupported: Boolean
    get() = Settings.enableQuic && isAtLeastQ
