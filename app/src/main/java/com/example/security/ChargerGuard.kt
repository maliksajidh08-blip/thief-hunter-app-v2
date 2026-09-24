package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

enum class ChargerBreachType(val title: String, val message: String) {
    UNPLUGGED(
        "Charger Disconnected!",
        "Someone disconnected the charger while Public Charger Guard was active!"
    ),
    MOVED_WHILE_CHARGING(
        "Phone Lifted from Charging Station!",
        "Unauthorized movement detected while phone was charging in public space!"
    )
}

enum class ChargerGuardEnvironment(val label: String, val icon: String, val description: String) {
    AIRPORT("Airport Terminal", "✈️", "High-traffic charging station security"),
    CAFE("Cafe / Restaurant", "☕", "Table charging station protection"),
    OFFICE("Shared Office", "💼", "Desk & co-working space guard"),
    PUBLIC_HUB("Public Charging Hub", "🔋", "Public USB charging kiosk guard")
}

data class ChargerGuardState(
    val isActive: Boolean = false,
    val isCharging: Boolean = false,
    val environment: ChargerGuardEnvironment = ChargerGuardEnvironment.AIRPORT,
    val baselineAcceleration: Float = 9.8f,
    val lastBreach: ChargerBreachType? = null,
    val statusMessage: String = "Standby - Ready for public charging"
)

/**
 * Enhanced Public Charging Protection Engine
 * Safeguards devices charging in airports, cafes, libraries, and public kiosks.
 * Triggers instant alarm if charger is pulled or device is shifted.
 */
class ChargerGuard(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _guardState = MutableStateFlow(ChargerGuardState())
    val guardState: StateFlow<ChargerGuardState> = _guardState.asStateFlow()

    var onBreachDetected: ((ChargerBreachType) -> Unit)? = null

    private var lastRecordedAccel = 9.8f
    private var isSensorRegistered = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _guardState.update { it.copy(isCharging = true, statusMessage = "Charging connected & secured") }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _guardState.update { it.copy(isCharging = false) }
                    if (_guardState.value.isActive) {
                        triggerBreach(ChargerBreachType.UNPLUGGED)
                    }
                }
            }
        }
    }

    init {
        checkInitialPowerState()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        try {
            context.registerReceiver(powerReceiver, filter)
        } catch (_: Exception) {}
    }

    private fun checkInitialPowerState() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        _guardState.update { it.copy(isCharging = isCharging) }
    }

    fun activate(environment: ChargerGuardEnvironment = ChargerGuardEnvironment.AIRPORT) {
        checkInitialPowerState()
        _guardState.update {
            it.copy(
                isActive = true,
                environment = environment,
                lastBreach = null,
                statusMessage = "Charger Guard ACTIVE (${environment.label}) - Protection Armed"
            )
        }
        startMotionMonitoring()
    }

    fun deactivate() {
        _guardState.update {
            it.copy(
                isActive = false,
                lastBreach = null,
                statusMessage = "Charger Guard Disarmed"
            )
        }
        stopMotionMonitoring()
    }

    fun setEnvironment(env: ChargerGuardEnvironment) {
        _guardState.update { it.copy(environment = env) }
    }

    private fun startMotionMonitoring() {
        if (!isSensorRegistered && accelerometer != null) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isSensorRegistered = true
        }
    }

    private fun stopMotionMonitoring() {
        if (isSensorRegistered) {
            sensorManager?.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    private fun triggerBreach(breach: ChargerBreachType) {
        _guardState.update {
            it.copy(
                lastBreach = breach,
                statusMessage = "BREACH: ${breach.title}"
            )
        }
        onBreachDetected?.invoke(breach)
    }

    fun simulateUnplugBreach() {
        if (_guardState.value.isActive) {
            triggerBreach(ChargerBreachType.UNPLUGGED)
        }
    }

    fun simulateMotionBreach() {
        if (_guardState.value.isActive) {
            triggerBreach(ChargerBreachType.MOVED_WHILE_CHARGING)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_guardState.value.isActive) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]
            val total = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()

            val delta = kotlin.math.abs(total - lastRecordedAccel)
            lastRecordedAccel = total

            // If phone moved while resting on public charging surface
            if (delta > 2.8f) {
                triggerBreach(ChargerBreachType.MOVED_WHILE_CHARGING)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cleanup() {
        stopMotionMonitoring()
        try {
            context.unregisterReceiver(powerReceiver)
        } catch (_: Exception) {}
    }
}
