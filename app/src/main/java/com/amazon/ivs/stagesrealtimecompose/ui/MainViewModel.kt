package com.amazon.ivs.stagesrealtimecompose.ui

import androidx.lifecycle.ViewModel
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.PreferencesHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    val destination = NavigationHandler.destination
    val dialogDestination = NavigationHandler.dialogDestination
    val isLoading = NavigationHandler.isLoading

    init {
        val startDestination = if (PreferencesHandler.session != null) {
            Destination.Landing()
        } else {
            Destination.Splash
        }
        NavigationHandler.goTo(startDestination)
    }
}
