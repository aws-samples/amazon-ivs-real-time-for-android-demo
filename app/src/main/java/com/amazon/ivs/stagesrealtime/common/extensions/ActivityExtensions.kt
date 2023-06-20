package com.amazon.ivs.stagesrealtime.common.extensions

import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.getCurrentFragment() =
    supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
