package com.hippo.ehviewer

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

val collectScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun Flow<Unit>.observe(func: (Unit) -> Unit) = collectScope.launch { collect(func) }

fun updateWhenLocaleChanges() {
    val newValue = Settings.language
    if ("system" == newValue) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newValue))
    }
}
