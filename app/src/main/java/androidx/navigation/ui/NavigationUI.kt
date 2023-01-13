package androidx.navigation.ui

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions

fun onNavDestinationSelected2(item: MenuItem, navController: NavController): Boolean {
    val builder = NavOptions.Builder().setLaunchSingleTop(true)
    if (
        navController.currentDestination!!.parent!!.findNode(item.itemId)
                is ActivityNavigator.Destination
    ) {
        builder.setEnterAnim(R.anim.nav_default_enter_anim)
            .setExitAnim(R.anim.nav_default_exit_anim)
            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
    } else {
        builder.setEnterAnim(com.hippo.ehviewer.R.anim.scene_open_enter)
            .setExitAnim(com.hippo.ehviewer.R.anim.scene_open_exit)
            .setPopEnterAnim(com.hippo.ehviewer.R.anim.scene_close_enter)
            .setPopExitAnim(com.hippo.ehviewer.R.anim.scene_close_exit)
    }
    if (item.order and Menu.CATEGORY_SECONDARY == 0) {
        builder.setPopUpTo(
            navController.graph.findStartDestination().id,
            inclusive = false,
            saveState = false
        )
    }
    val options = builder.build()
    return try {
        // TODO provide proper API instead of using Exceptions as Control-Flow.
        navController.navigate(item.itemId, null, options)
        // Return true only if the destination we've navigated to matches the MenuItem
        navController.currentDestination?.matchDestination(item.itemId) == true
    } catch (e: IllegalArgumentException) {
        false
    }
}

fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }