package com.amazon.ivs.stagesrealtime.common

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import java.util.*

class PermissionHandler(private val context: AppCompatActivity) {
    private val permissionRequestHistory = hashMapOf<Int, (a: Boolean) -> Unit>()

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED

    private fun arePermissionsGranted(): Boolean = hasCameraPermission() && hasRecordAudioPermission()

    private fun askForPermissions() {
        run {
            val permissions = arrayListOf<String>()
            if (!hasCameraPermission()) {
                permissions.add(Manifest.permission.CAMERA)
            }
            if (!hasRecordAudioPermission()) {
                permissions.add(Manifest.permission.RECORD_AUDIO)
            }
            if (permissions.isNotEmpty()) {
                val requestCode = Date().time.toInt().low16bits()
                ActivityCompat.requestPermissions(context, permissions.toTypedArray(), requestCode)
            }
        }
    }

    fun checkPermissions() {
        if (!arePermissionsGranted()) {
            askForPermissions()
        }
    }

    fun onPermissionResult(requestCode: Int, grantResults: IntArray) {
        permissionRequestHistory[requestCode]?.run {
            this(grantResults.isNotEmpty() && grantResults[0] == PermissionChecker.PERMISSION_GRANTED)
            permissionRequestHistory.remove(requestCode)
        }
    }

    private fun Int.low16bits() = this and 0xFFFF
}
