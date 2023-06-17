package androidx.navigation.ui

import android.annotation.SuppressLint
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavOptions

@SuppressLint("PrivateResource")
fun onNavDestinationSelected2(item: MenuItem, navController: NavController): Boolean {
    if (navController.currentDestination?.matchDestination(item.itemId) == true) return true
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
        builder.setEnterAnim(androidx.fragment.R.animator.fragment_open_enter)
            .setExitAnim(androidx.fragment.R.animator.fragment_open_exit)
            .setPopEnterAnim(androidx.fragment.R.animator.fragment_close_enter)
            .setPopExitAnim(androidx.fragment.R.animator.fragment_close_exit)
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
