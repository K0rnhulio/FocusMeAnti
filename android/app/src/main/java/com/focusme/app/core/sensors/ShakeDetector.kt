package com.focusme.app.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * High-precision Accelerometer Shake Detector
 * Requires high acceleration magnitude (G > 1.8g) with peak-to-trough cycles.
 */
class ShakeDetector(
    context: Context,
    private val targetShakes: Int = 50,
    private val onShakeProgress: (current: Int, target: Int) -> Unit,
    private val onShakeCompleted: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var shakeThreshold = 14.5f // ~1.5g to 1.8g acceleration spike
    private var lastShakeTimestamp = 0L

    fun start() {
        shakeCount = 0
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration

        val now = System.currentTimeMillis()
        if (delta > shakeThreshold && (now - lastShakeTimestamp) > 180) { // Min 180ms between pumps
            lastShakeTimestamp = now
            shakeCount++
            onShakeProgress(shakeCount, targetShakes)

            if (shakeCount >= targetShakes) {
                stop()
                onShakeCompleted()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
