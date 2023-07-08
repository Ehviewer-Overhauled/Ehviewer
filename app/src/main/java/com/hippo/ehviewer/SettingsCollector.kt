package com.hippo.ehviewer

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.core.os.LocaleListCompat
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dailycheck.updateDailyCheckWork
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.preferences.PrefDelegate

private val collectScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
fun <T, R : PrefDelegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { changesFlow().collect(func) } }
fun <T, R : Settings.Delegate<T>> R.observed(func: (Unit) -> Unit) = apply { collectScope.launch { flowGetter().collect(func) } }
fun <T, R : Settings.Delegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { flowGetter().collect { flow.emit(Unit) } } }
fun <T, R : PrefDelegate<T>> R.emitTo(flow: MutableSharedFlow<Unit>) = apply { collectScope.launch { changesFlow().collect { flow.emit(Unit) } } }

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

fun updateWhenAmoledModeChanges() {
    collectScope.launch {
        delay(100) // Avoid recompose being cancelled
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_AUTO_BATTERY)
        AppCompatDelegate.setDefaultNightMode(Settings.theme)
    }
}

fun updateWhenRequestNewsChanges() {
    updateDailyCheckWork(appCtx)
}

fun updateWhenGallerySiteChanges() {
    if (!Settings.needSignIn && EhCookieStore.hasSignedIn()) {
        collectScope.launchIO {
            runCatching {
                EhEngine.getUConfig()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}

fun updateWhenTagTranslationChanges() {
    collectScope.launchIO {
        runCatching {
            EhTagDatabase.update()
        }.onFailure {
            it.printStackTrace()
        }
    }
}
