package com.amazon.ivs.stagesrealtime.common.extensions

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.amazon.ivs.stagesrealtime.R

/**
 * Navigates to another fragment using the given [action].
 *
 * You should only use the current Fragment directions to navigate to any other destination, those that are defined
 * in a XML navigation graph file, for this project being navigation_main.xml.
 *
 * This method under the hood handles a bug with Android Navigation, where multiple clicks in rapid succession
 * could result in duplicate navigation calls, some which may be coming from the target destination fragment
 * and be invalid, causing a crash.
 *
 * For example, if you are going from A -> B, the initial NavController navigate call happens from
 * the A screen, but the repeated one may happen from B, and if
 * there is no navigation action defined for B -> B, it causes a crash with a message
 * of "no action exists on this destination".
 */
fun Fragment.navigate(action: NavDirections) {
    val navController = findNavController()
    val destinationId = navController.currentDestination?.getAction(action.actionId)?.destinationId

    navController.currentDestination?.let { destination ->
        val currentDestination = when (destination) {
            is NavGraph -> destination
            else -> destination.parent
        }
        if (destinationId != null) {
            currentDestination?.findNode(destinationId)?.let { navController.navigate(action) }
        }
    }
}

val Fragment.navController get() =
    (requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_host_view) as NavHostFragment).navController
