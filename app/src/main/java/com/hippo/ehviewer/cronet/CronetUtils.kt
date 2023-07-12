package com.hippo.ehviewer.cronet

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.util.isAtLeastQ

val isCronetSupported: Boolean
    get() = Settings.enableQuic && isAtLeastQ
