package com.focusme.app.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * 25-Step Bed-Exit Tracker using hardware step detector or step counter.
 */
class StepTracker(
    context: Context,
    private val targetSteps: Int = 25,
    private val onStepProgress: (current: Int, target: Int) -> Unit,
    private val onStepCompleted: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private var stepCount = 0

    fun start() {
        stepCount = 0
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++
            onStepProgress(stepCount, targetSteps)

            if (stepCount >= targetSteps) {
                stop()
                onStepCompleted()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
