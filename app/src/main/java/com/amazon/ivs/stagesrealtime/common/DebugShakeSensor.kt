package com.amazon.ivs.stagesrealtime.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A sensor class to be used inside of a [Fragment] for listening to the user device's shakes, that
 * triggers the [onShaken] callback if the provided [isEnabled] condition is met.
 *
 * It automatically unregisters and registers listeners based on the given lifecycle (usually a Fragment).
 */
class DebugShakeSensor(
    private val isEnabled: (force: Float) -> Boolean,
    private val onShaken: () -> Unit,
    private val context: Context
) : SensorEventListener, DefaultLifecycleObserver {
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
                Timber.d("Shake detected: $force, count: $shakeCount")
            }
            if (isEnabled(force)) {
                shakeCount++
                if (shakeCount > SHAKE_COUNT_THRESHOLD) {
                    shakeCount = 0
                    onShaken()
                }
            } else {
                shakeCount = 0
            }
            lastShakeTime = currentShakeTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Ignored */ }
}
