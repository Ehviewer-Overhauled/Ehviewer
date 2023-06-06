package com.hippo.ehviewer

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hippo.ehviewer.dailycheck.updateDailyCheckWork
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.preferences.PrefDelegate

val collectScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
fun <T, R : PrefDelegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { changesFlow().collect(func) } }
fun <T, R : Settings.Delegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { flowGetter().collect(func) } }

fun updateWhenLocaleChanges() {
    val newValue = Settings.language
    if ("system" == newValue) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newValue))
    }
}

fun updateWhenKeepMediaStatusChanges() {
    collectScope.launchIO {
        runCatching {
            keepNoMediaFileStatus()
        }
    }
}

fun updateWhenThemeChanges() {
    collectScope.launch {
        delay(100) // Avoid recompose being cancelled
        AppCompatDelegate.setDefaultNightMode(Settings.theme)
    }
}

fun updateWhenRequestNewsChanges() {
    updateDailyCheckWork(appCtx)
}
