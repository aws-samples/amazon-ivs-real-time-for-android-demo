package com.amazon.ivs.stagesrealtimecompose.core.handlers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.amazon.ivs.stagesrealtimecompose.appContext
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

private const val SHAKE_TIME_THRESHOLD = 100L
private const val SHAKE_COUNT_THRESHOLD = 2
private const val SHAKE_FORCE_THRESHOLD = 9f

object ShakeHandler : SensorEventListener, DefaultLifecycleObserver {
    private val sensorManager: SensorManager by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var lastShakeTime = 0L
    private var shakeCount = 0

    fun setup(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val force = sqrt(x.pow(2) + y.pow(2) + z.pow(2)) - SensorManager.GRAVITY_EARTH

        val currentShakeTime = System.currentTimeMillis()
        val shakeDelta = currentShakeTime - lastShakeTime
        if (shakeDelta > SHAKE_TIME_THRESHOLD) {
            if (force > 1) {
                Timber.Forest.d("Shake detected: $force, count: $shakeCount")
            }
            val currentStage = StageHandler.currentStage
            if (currentStage != null && !currentStage.isLoading && force > SHAKE_FORCE_THRESHOLD) {
                shakeCount++
                if (shakeCount > SHAKE_COUNT_THRESHOLD) {
                    shakeCount = 0
                    NavigationHandler.showDialog(DialogDestination.Debug)
                }
            } else {
                shakeCount = 0
            }
            lastShakeTime = currentShakeTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Ignored */ }
}
