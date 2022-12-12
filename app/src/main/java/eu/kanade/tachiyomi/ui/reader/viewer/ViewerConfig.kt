package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.preference.PreferenceValues.TappingInvertMode
import kotlinx.coroutines.CoroutineScope

/**
 * Common configuration for all viewers.
 */
abstract class ViewerConfig(private val scope: CoroutineScope) {

    var imagePropertyChangedListener: (() -> Unit)? = null

    var navigationModeChangedListener: (() -> Unit)? = null

    var tappingInverted = TappingInvertMode.NONE
    var longTapEnabled = true
    var usePageTransitions = false
    var doubleTapAnimDuration = 500
    var volumeKeysEnabled = false
    var volumeKeysInverted = false
    var trueColor = false
    var alwaysShowChapterTransition = true
    var navigationMode = 0
        protected set

    var forceNavigationOverlay = false

    var navigationOverlayOnStart = false

    var dualPageSplit = false
        protected set

    var dualPageInvert = false
        protected set

    abstract var navigator: ViewerNavigation
        protected set

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)
}
