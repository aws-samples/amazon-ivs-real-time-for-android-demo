package com.amazon.ivs.stagesrealtime.ui

import androidx.fragment.app.Fragment

/**
 * Indicates that the given [Fragment] will be handling the back press instead of the standard
 * implementation in [MainActivity.onNavigateUp].
 */
interface BackHandler {
    /** Triggered function on a back press from the device. */
    fun handleBackPress()
}
