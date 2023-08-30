package com.amazon.ivs.stagesrealtime.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.amazon.ivs.stagesrealtime.common.PermissionHandler
import com.amazon.ivs.stagesrealtime.common.extensions.getCurrentFragment
import com.amazon.ivs.stagesrealtime.databinding.ActivityMainBinding
import com.amazon.ivs.stagesrealtime.ui.lobby.LobbyFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val permissionHandler by lazy { PermissionHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
            NOTE: for the sample application, we have simplified the checking of permissions
            to be done all at startup,
            but you should do this process whenever the user gets to the features needing these
            and handle those flows gracefully in your production applications.
         */
        permissionHandler.checkPermissions()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onSupportNavigateUp()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onPermissionResult(requestCode, grantResults)
    }

    override fun onSupportNavigateUp(): Boolean {
        when (val currentFragment = getCurrentFragment()) {
            is BackHandler -> currentFragment.handleBackPress()
            is LobbyFragment -> finish()
        }
        return false
    }
}
