package com.amazon.ivs.stagesrealtimecompose.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import timber.log.Timber

@Composable
fun PermissionRequester() {
    val context = LocalContext.current
    var cameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var micPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToGrant = mutableListOf<String>()
    if (!cameraPermission) permissionsToGrant.add(Manifest.permission.CAMERA)
    if (!micPermission) permissionsToGrant.add(Manifest.permission.RECORD_AUDIO)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted ->
        cameraPermission = permissionsGranted[Manifest.permission.CAMERA] ?: cameraPermission
        micPermission = permissionsGranted[Manifest.permission.RECORD_AUDIO] ?: micPermission
        Timber.d("Permissions granted: $cameraPermission, $micPermission")
    }

    LaunchedEffect(key1 = permissionsToGrant) {
        if (permissionsToGrant.isEmpty()) return@LaunchedEffect

        permissionLauncher.launch(permissionsToGrant.toTypedArray())
    }
}
