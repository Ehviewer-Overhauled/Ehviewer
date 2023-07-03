package com.hippo.ehviewer.cronet

import android.os.Build
import com.hippo.ehviewer.Settings

val isCronetSupported: Boolean
    get() = Settings.enableQuic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
