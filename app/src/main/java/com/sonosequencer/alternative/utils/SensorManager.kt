package com.sonosequencer.alternative.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.acos
import kotlin.math.sqrt

class SensorManager(private val context: Context) {
    
    private val sensorManager: AndroidSensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    }
    
    private val _angleChannel = Channel<Float>(Channel.CONFLATED)
    val angleFlow: Flow<Float> = _angleChannel.receiveAsFlow()
    
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.size)
                    calculateAngle()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerValues, 0, event.values.size)
                    calculateAngle()
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Not used
        }
    }
    
    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }
    
    fun startAngleMeasurement() {
        accelerometer?.let { accel ->
            sensorManager.registerListener(sensorListener, accel, AndroidSensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.let { mag ->
            sensorManager.registerListener(sensorListener, mag, AndroidSensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    fun stopAngleMeasurement() {
        sensorManager.unregisterListener(sensorListener)
    }
    
    private fun calculateAngle() {
        if (accelerometerValues != null && magnetometerValues != null) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            
            // Calculate rotation matrix
            if (AndroidSensorManager.getRotationMatrix(
                    rotationMatrix,
                    inclinationMatrix,
                    accelerometerValues,
                    magnetometerValues
                )) {
                
                val orientationValues = FloatArray(3)
                AndroidSensorManager.getOrientation(rotationMatrix, orientationValues)
                
                // orientationValues[0] = azimuth (rotation around Z axis)
                // orientationValues[1] = pitch (rotation around X axis)
                // orientationValues[2] = roll (rotation around Y axis)
                
                // For speaker placement, we're interested in the azimuth (compass direction)
                // and pitch (tilt forward/backward)
                
                // Calculate the angle between phone front and speaker front
                // When phone is placed on speaker with fronts aligned, pitch should be ~0
                // and azimuth gives the rotation
                
                val azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
                
                // For simplicity, we'll use the pitch as the main angle
                // (tilt forward/backward from horizontal)
                // In practice, you'd want to calibrate this based on how the phone is placed
                
                // Calculate the angle between phone and speaker
                // This is a simplified calculation
                val angle = calculatePhoneSpeakerAngle(accelerometerValues)
                
                _angleChannel.trySend(angle)
            }
        }
    }
    
    private fun calculatePhoneSpeakerAngle(accelValues: FloatArray): Float {
        // Calculate the angle based on accelerometer values
        // This assumes the phone is placed flat on the speaker
        
        val x = accelValues[0]
        val y = accelValues[1]
        val z = accelValues[2]
        
        // Calculate the tilt angle from horizontal
        // Using the formula: angle = arccos(z / sqrt(x^2 + y^2 + z^2))
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        val tiltAngle = if (magnitude > 0) {
            Math.toDegrees(acos(z / magnitude)).toFloat()
        } else {
            0f
        }
        
        // For speaker angle calculation, we want to know the angle between
        // the phone's front and the speaker's front
        // This is more complex and would require calibration
        
        // For now, return the tilt angle as a simplified measure
        // In practice, you'd want to implement a more sophisticated algorithm
        // that takes into account the phone's orientation relative to the speaker
        
        return tiltAngle
    }
    
    fun isSensorAvailable(sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }
    
    fun hasRequiredSensors(): Boolean {
        return isSensorAvailable(Sensor.TYPE_ACCELEROMETER) &&
               isSensorAvailable(Sensor.TYPE_MAGNETIC_FIELD)
    }
}
