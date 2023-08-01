package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import splitties.systemservices.windowManager
import kotlin.math.roundToInt
import com.hippo.ehviewer.Settings as AppSettings

/**
 * Returns the color for the given attribute.
 *
 * @param resource the attribute.
 * @param alphaFactor the alpha number [0,1].
 */
@ColorInt fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(resource))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()

    if (alphaFactor < 1f) {
        val alpha = (color.alpha * alphaFactor).roundToInt()
        return Color.argb(alpha, color.red, color.green, color.blue)
    }

    return color
}

/**
 * Converts to px.
 */
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)

val Resources.isLTR
    get() = configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR

val Context.displayCompat: Display?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }

/** Gets the duration multiplier for general animations on the device
 * @see Settings.Global.ANIMATOR_DURATION_SCALE
 */
val Context.animatorDurationScale: Float
    get() = Settings.Global.getFloat(this.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private const val TABLET_UI_REQUIRED_SCREEN_WIDTH_DP = 720

// some tablets have screen width like 711dp = 1600px / 2.25
private const val TABLET_UI_MIN_SCREEN_WIDTH_PORTRAIT_DP = 700

// make sure icons on the nav rail fit
private const val TABLET_UI_MIN_SCREEN_WIDTH_LANDSCAPE_DP = 600

fun Context.isTabletUi(): Boolean {
    return resources.configuration.isTabletUi()
}

fun Configuration.isTabletUi(): Boolean {
    return smallestScreenWidthDp >= TABLET_UI_REQUIRED_SCREEN_WIDTH_DP
}

fun Configuration.isAutoTabletUiAvailable(): Boolean {
    return smallestScreenWidthDp >= TABLET_UI_MIN_SCREEN_WIDTH_LANDSCAPE_DP
}

/**
 * Returns true if current context is in night mode
 */
fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Gets system's config_navBarNeedsScrim boolean flag added in Android 10, defaults to true.
 */
fun Context.isNavigationBarNeedsScrim(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        InternalResourceHelper.getBoolean(this, "config_navBarNeedsScrim", true)
}

/**
 * Creates night mode Context depending on reader theme/background
 *
 * Context wrapping method obtained from AppCompatDelegateImpl
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:appcompat/appcompat/src/main/java/androidx/appcompat/app/AppCompatDelegateImpl.java;l=348;drc=e28752c96fc3fb4d3354781469a1af3dbded4898
 */
fun Context.createReaderThemeContext(): Context {
    val isDarkBackground = when (ReaderPreferences.readerTheme().get()) {
        1, 2 -> true // Black, Gray
        3 -> applicationContext.isNightMode() // Automatic bg uses activity background by default
        else -> false // White
    }
    val expected = if (isDarkBackground) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != expected) {
        val overrideConf = Configuration()
        overrideConf.setTo(resources.configuration)
        overrideConf.uiMode = overrideConf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or expected

        val wrappedContext = ContextThemeWrapper(this, R.style.AppTheme)
        wrappedContext.applyOverrideConfiguration(overrideConf)
        val resId = if (AppSettings.blackDarkTheme && isDarkBackground) R.style.ThemeOverlay_Black else R.style.ThemeOverlay
        wrappedContext.theme.applyStyle(resId, true)
        return wrappedContext
    }
    return this
}

inline fun Any.logcat(priority: Int = Log.DEBUG, tag: String? = null, message: () -> String) {
    Log.println(priority, tag ?: javaClass.simpleName, message())
}
