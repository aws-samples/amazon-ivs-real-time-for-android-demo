package com.amazon.ivs.stagesrealtime.common.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.amazon.ivs.stagesrealtime.R

fun Fragment.navigate(action: NavDirections) = findNavController().navigate(action)

val Fragment.navController get() =
    (requireActivity().supportFragmentManager.findFragmentById(R.id.fragment_host_view) as NavHostFragment).navController

fun NavController.noDuplicateNavigate(@IdRes destinationId: Int, args: Bundle? = null) {
    if (currentDestination?.matchDestination(destinationId) == true) return
    navigate(destinationId, args)
}

private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }
