package com.sonosequencer.alternative.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Manages device sensors for angle measurement.
 * 
 * This class uses the accelerometer and magnetometer to calculate
 * the device's orientation and tilt angle when placed on a speaker.
 */
class SensorManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SensorManager"
        private const val SENSOR_DELAY = AndroidSensorManager.SENSOR_DELAY_NORMAL
        private const val MIN_ANGLE_CHANGE = 0.5f // Minimum angle change to trigger update (degrees)
    }
    
    private val sensorManager: AndroidSensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    }
    
    private val _angleChannel = Channel<Float>(Channel.CONFLATED)
    val angleFlow: Flow<Float> = _angleChannel.receiveAsFlow()
    
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    private var lastReportedAngle: Float = 0f
    
    private var isMeasuring = false
    
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.size)
                    calculateAndReportAngle()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerValues, 0, event.values.size)
                    calculateAndReportAngle()
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d(TAG, "Sensor accuracy changed: ${sensor.name} -> $accuracy")
        }
    }
    
    init {
        // Initialize sensor references
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        Log.d(TAG, "SensorManager initialized")
        Log.d(TAG, "Accelerometer available: ${accelerometer != null}")
        Log.d(TAG, "Magnetometer available: ${magnetometer != null}")
    }
    
    /**
     * Starts angle measurement using device sensors.
     * 
     * @return true if sensors are available and measurement started, false otherwise
     */
    fun startAngleMeasurement(): Boolean {
        if (isMeasuring) {
            Log.w(TAG, "Angle measurement already in progress")
            return true
        }
        
        if (!hasRequiredSensors()) {
            Log.e(TAG, "Required sensors not available")
            return false
        }
        
        try {
            accelerometer?.let { accel ->
                sensorManager.registerListener(sensorListener, accel, SENSOR_DELAY)
                Log.d(TAG, "Registered accelerometer listener")
            }
            magnetometer?.let { mag ->
                sensorManager.registerListener(sensorListener, mag, SENSOR_DELAY)
                Log.d(TAG, "Registered magnetometer listener")
            }
            
            isMeasuring = true
            lastReportedAngle = 0f
            Log.d(TAG, "Angle measurement started")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting angle measurement", e)
            return false
        }
    }
    
    /**
     * Stops angle measurement.
     */
    fun stopAngleMeasurement() {
        if (!isMeasuring) {
            Log.d(TAG, "Angle measurement not in progress")
            return
        }
        
        try {
            sensorManager.unregisterListener(sensorListener)
            isMeasuring = false
            Log.d(TAG, "Angle measurement stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping angle measurement", e)
        }
    }
    
    /**
     * Calculates the angle based on sensor values and reports it if changed significantly.
     */
    private fun calculateAndReportAngle() {
        if (accelerometerValues.isEmpty() || magnetometerValues.isEmpty()) {
            return
        }
        
        try {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            
            // Calculate rotation matrix using sensor fusion
            if (AndroidSensorManager.getRotationMatrix(
                    rotationMatrix,
                    inclinationMatrix,
                    accelerometerValues,
                    magnetometerValues
                )) {
                
                val orientationValues = FloatArray(3)
                AndroidSensorManager.getOrientation(rotationMatrix, orientationValues)
                
                // orientationValues[0] = azimuth (rotation around Z axis) in radians
                // orientationValues[1] = pitch (rotation around X axis) in radians
                // orientationValues[2] = roll (rotation around Y axis) in radians
                
                // Convert to degrees
                val azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
                
                // For speaker placement, we're primarily interested in the pitch
                // (tilt forward/backward from horizontal)
                // When phone is placed flat on speaker, pitch should be ~0
                
                // Calculate the magnitude of tilt
                val tiltAngle = calculateTiltAngle(accelerometerValues)
                
                // Use pitch as the primary angle for speaker placement
                val currentAngle = Math.abs(pitch)
                
                // Only report if angle changed significantly
                if (Math.abs(currentAngle - lastReportedAngle) >= MIN_ANGLE_CHANGE || 
                    lastReportedAngle == 0f) {
                    lastReportedAngle = currentAngle
                    _angleChannel.trySend(currentAngle)
                    Log.d(TAG, "Angle updated: ${currentAngle}° (pitch: ${pitch}°, roll: ${roll}°, azimuth: ${azimuth}°)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating angle", e)
        }
    }
    
    /**
     * Calculates the tilt angle from horizontal using accelerometer values.
     * 
     * @param accelValues Accelerometer values (x, y, z)
     * @return Tilt angle in degrees
     */
    private fun calculateTiltAngle(accelValues: FloatArray): Float {
        if (accelValues.size < 3) return 0f
        
        val x = accelValues[0]
        val y = accelValues[1]
        val z = accelValues[2]
        
        // Calculate the tilt angle from horizontal
        // Using the formula: angle = arccos(z / sqrt(x^2 + y^2 + z^2))
        try {
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            if (magnitude > 0) {
                val angleRad = Math.acos(z / magnitude)
                return Math.toDegrees(angleRad).toFloat()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating tilt angle", e)
        }
        
        return 0f
    }
    
    /**
     * Checks if the device has the required sensors.
     */
    fun hasRequiredSensors(): Boolean {
        return isSensorAvailable(Sensor.TYPE_ACCELEROMETER) &&
               isSensorAvailable(Sensor.TYPE_MAGNETIC_FIELD)
    }
    
    /**
     * Checks if a specific sensor type is available.
     */
    fun isSensorAvailable(sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }
    
    /**
     * Gets the list of available sensors.
     */
    fun getAvailableSensors(): List<Sensor> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
    }
    
    /**
     * Checks if angle measurement is currently in progress.
     */
    fun isMeasuring(): Boolean = isMeasuring
    
    /**
     * Gets the last reported angle.
     */
    fun getLastAngle(): Float = lastReportedAngle
    
    /**
     * Resets the sensor manager.
     */
    fun reset() {
        stopAngleMeasurement()
        lastReportedAngle = 0f
    }
}
