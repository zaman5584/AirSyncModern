package com.example.airsyncmodern.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Pure Kotlin implementation of Air Mouse sensor engine.
 * Replaces legacy 32-bit libairmouse_jni.so with modern Android SensorManager.
 * 
 * Features:
 * - Gyroscope-based pointer movement
 * - Accelerometer for motion detection
 * - Lifecycle-aware sensor registration/unregistration
 * - Configurable sensitivity and smoothing
 * - Hilt injectable singleton
 * 
 * This completely eliminates the dependency on native JNI libraries,
 * making the app compatible with 64-bit architectures and modern Android versions.
 */
@Singleton
class AirMouseSensorEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "AirMouseSensorEngine"
        
        // Sensor sampling rates
        private const val SENSOR_DELAY_MICROS = 16000 // ~60Hz
        
        // Movement thresholds and sensitivity
        private const val GYRO_SENSITIVITY = 20f
        private const val GYRO_DEADZONE = 0.01f
        private const val SMOOTHING_FACTOR = 0.7f
        
        // Accelerometer shake detection
        private const val SHAKE_THRESHOLD = 15f
        private const val SHAKE_COOLDOWN_MS = 1000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // State flows
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _cursorDelta = MutableStateFlow(Pair(0f, 0f))
    val cursorDelta: StateFlow<Pair<Float, Float>> = _cursorDelta.asStateFlow()

    private val _shakeDetected = MutableStateFlow(false)
    val shakeDetected: StateFlow<Boolean> = _shakeDetected.asStateFlow()

    // Movement smoothing
    private var smoothedX = 0f
    private var smoothedY = 0f
    
    // Shake detection
    private var lastShakeTime = 0L
    private var lastAcceleration = 0f

    // Sensor availability
    val isGyroscopeAvailable: Boolean
        get() = gyroscopeSensor != null

    val isAccelerometerAvailable: Boolean
        get() = accelerometerSensor != null

    /**
     * Starts the Air Mouse sensor engine.
     * Registers gyroscope and accelerometer listeners.
     */
    fun start() {
        if (_isActive.value) {
            Log.w(TAG, "Air Mouse already active")
            return
        }

        if (!isGyroscopeAvailable) {
            Log.e(TAG, "Gyroscope not available on this device")
            return
        }

        Log.d(TAG, "Starting Air Mouse sensor engine")

        // Register gyroscope for pointer movement
        gyroscopeSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SENSOR_DELAY_MICROS
            )
            if (registered) {
                Log.d(TAG, "Gyroscope registered successfully")
            } else {
                Log.e(TAG, "Failed to register gyroscope")
            }
        }

        // Register accelerometer for shake detection
        accelerometerSensor?.let { sensor ->
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            if (registered) {
                Log.d(TAG, "Accelerometer registered successfully")
            } else {
                Log.e(TAG, "Failed to register accelerometer")
            }
        }

        _isActive.value = true
        resetSmoothing()
    }

    /**
     * Stops the Air Mouse sensor engine.
     * Unregisters all sensor listeners.
     */
    fun stop() {
        if (!_isActive.value) {
            Log.w(TAG, "Air Mouse already inactive")
            return
        }

        Log.d(TAG, "Stopping Air Mouse sensor engine")
        
        sensorManager.unregisterListener(this)
        _isActive.value = false
        _cursorDelta.value = Pair(0f, 0f)
        resetSmoothing()
    }

    /**
     * Toggles Air Mouse on/off
     */
    fun toggle() {
        if (_isActive.value) {
            stop()
        } else {
            start()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used, but required by SensorEventListener
    }

    /**
     * Processes gyroscope data to calculate cursor movement.
     * 
     * Gyroscope returns angular velocity (rad/s) around each axis:
     * - X axis: rotation around X (pitch) -> affects vertical cursor movement
     * - Y axis: rotation around Y (roll) -> affects horizontal cursor movement
     * - Z axis: rotation around Z (yaw) -> not used for cursor
     */
    private fun handleGyroscope(event: SensorEvent) {
        // Extract gyroscope values
        val rotationX = event.values[0] // Pitch (affects Y cursor movement)
        val rotationY = event.values[1] // Roll (affects X cursor movement)

        // Apply deadzone to filter out noise
        val filteredX = if (abs(rotationY) > GYRO_DEADZONE) rotationY else 0f
        val filteredY = if (abs(rotationX) > GYRO_DEADZONE) rotationX else 0f

        // Apply sensitivity scaling
        var deltaX = filteredX * GYRO_SENSITIVITY
        var deltaY = -filteredY * GYRO_SENSITIVITY // Invert Y for natural movement

        // Apply exponential smoothing to reduce jitter
        smoothedX = SMOOTHING_FACTOR * smoothedX + (1 - SMOOTHING_FACTOR) * deltaX
        smoothedY = SMOOTHING_FACTOR * smoothedY + (1 - SMOOTHING_FACTOR) * deltaY

        // Emit smoothed delta
        _cursorDelta.value = Pair(smoothedX, smoothedY)
    }

    /**
     * Processes accelerometer data to detect shake gestures.
     * A shake can be used to enable/disable air mouse mode.
     */
    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate total acceleration magnitude
        val acceleration = kotlin.math.sqrt(x * x + y * y + z * z)
        
        // Calculate acceleration delta (change rate)
        val delta = abs(acceleration - lastAcceleration)
        lastAcceleration = acceleration

        // Detect shake if delta exceeds threshold
        val currentTime = System.currentTimeMillis()
        if (delta > SHAKE_THRESHOLD && currentTime - lastShakeTime > SHAKE_COOLDOWN_MS) {
            Log.d(TAG, "Shake detected! Delta: $delta")
            _shakeDetected.value = true
            lastShakeTime = currentTime
            
            // Auto-reset shake flag after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _shakeDetected.value = false
            }, 500)
        }
    }

    /**
     * Resets movement smoothing state
     */
    private fun resetSmoothing() {
        smoothedX = 0f
        smoothedY = 0f
    }

    /**
     * Calibrates the sensor by resetting accumulated values
     */
    fun calibrate() {
        Log.d(TAG, "Calibrating sensors")
        resetSmoothing()
        _cursorDelta.value = Pair(0f, 0f)
    }

    /**
     * Returns the current sensor status
     */
    fun getStatus(): AirMouseStatus {
        return AirMouseStatus(
            isActive = _isActive.value,
            isGyroscopeAvailable = isGyroscopeAvailable,
            isAccelerometerAvailable = isAccelerometerAvailable,
            currentDelta = _cursorDelta.value
        )
    }
}

/**
 * Data class representing Air Mouse status
 */
data class AirMouseStatus(
    val isActive: Boolean,
    val isGyroscopeAvailable: Boolean,
    val isAccelerometerAvailable: Boolean,
    val currentDelta: Pair<Float, Float>
)
