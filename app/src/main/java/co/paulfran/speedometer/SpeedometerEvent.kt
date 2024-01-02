package co.paulfran.speedometer

sealed class SpeedometerEvent{
    object StartTracking: SpeedometerEvent()
    data class StartButtonEnabled(val enabled: Boolean): SpeedometerEvent()
    data class ChangeStartButtonText(val text: String): SpeedometerEvent()

    data class PauseButtonEnabled(val enabled: Boolean): SpeedometerEvent()
    object PauseTracking: SpeedometerEvent()

    object ResumeTracking:SpeedometerEvent()

    data class StopButtonEnabled(val enabled: Boolean): SpeedometerEvent()
    object StopTracking: SpeedometerEvent()

    data class ChangeLocation(val latitude: Double, val longitude: Double): SpeedometerEvent()
    data class ChangeSpeed(val speed: Double): SpeedometerEvent()

}