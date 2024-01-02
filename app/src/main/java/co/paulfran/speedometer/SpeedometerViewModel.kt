package co.paulfran.speedometer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import im.delight.android.location.SimpleLocation

class SpeedometerViewModel : ViewModel() {


    private val _isTracking: MutableLiveData<Boolean> = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _isPaused: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPaused: LiveData<Boolean> = _isPaused

    private val _isResumed: MutableLiveData<Boolean> = MutableLiveData(false)
    val isResumed: LiveData<Boolean> = _isResumed

    private val _travelledDistance: MutableLiveData<String> = MutableLiveData("")
    val travelledDistance: LiveData<String> = _travelledDistance

    private val _topSpeed: MutableLiveData<Int> = MutableLiveData(0)
    val topSpeed: LiveData<Int> = _topSpeed

    private val _currentSpeedKph: MutableLiveData<Double> = MutableLiveData(0.0)
    val currentSpeedKph: LiveData<Double> = _currentSpeedKph

    private val _currentLocation: MutableLiveData<SimpleLocation.Point> = MutableLiveData(SimpleLocation.Point(0.0, 0.0))
    val currentLocation: LiveData<SimpleLocation.Point> = _currentLocation

    private val _stopButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val stopButtonEnabled: LiveData<Boolean> = _stopButtonEnabled

    private val _startButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(true)
    val startButtonEnabled: LiveData<Boolean> = _startButtonEnabled

    private val _pauseButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val pauseButtonEnabled: LiveData<Boolean> = _pauseButtonEnabled

    private val _startButtonText: MutableLiveData<String> = MutableLiveData("Start")
    val startButtonText: LiveData<String> = _startButtonText


    private var travelledPath: MutableList<LatLng> = mutableListOf()


    fun onEvent(event: SpeedometerEvent) {
        when (event) {
            is SpeedometerEvent.ChangeSpeed -> {

                _currentSpeedKph.value = event.speed
                getTopSpeed(event.speed.toInt())
            }

            is SpeedometerEvent.ChangeLocation -> {
                _currentLocation.value = SimpleLocation.Point(event.latitude, event.longitude)
                travelledPath.add(element = LatLng(event.latitude, event.longitude))
            }

            is SpeedometerEvent.StartButtonEnabled -> {
                _startButtonEnabled.value = event.enabled
            }



            is SpeedometerEvent.ResumeTracking -> {
                _isPaused.value = false
                _isResumed.value = true
                _startButtonEnabled.value = false
                _pauseButtonEnabled.value = true
                _stopButtonEnabled.value = true
            }
            is SpeedometerEvent.PauseTracking -> {
                _isPaused.value = true
                _isResumed.value = false
                _pauseButtonEnabled.value = false
                _startButtonEnabled.value = true
                _stopButtonEnabled.value = true
                _currentSpeedKph.value = 0.0
                _startButtonText.value = "Resume"
            }

            is SpeedometerEvent.StartTracking -> {
                _isTracking.value = true
                _isPaused.value = false
                _travelledDistance.value = ""
                _startButtonEnabled.value = false
                _pauseButtonEnabled.value = true
                _stopButtonEnabled.value = true
            }



            is SpeedometerEvent.StopTracking -> {
                val pathTravelled = SphericalUtil.computeLength(travelledPath)
                _travelledDistance.value = pathTravelled.formatToDistance()
                _stopButtonEnabled.value = false
                _isPaused.value = false
                _startButtonEnabled.value = true
                _isTracking.value = false
                _startButtonText.value = "Start"
                _pauseButtonEnabled.value = false
                _currentSpeedKph.value = 0.0
                travelledPath = mutableListOf()
            }





            is SpeedometerEvent.StopButtonEnabled -> {
                _stopButtonEnabled.value = event.enabled
            }

            is SpeedometerEvent.PauseButtonEnabled -> {
                _pauseButtonEnabled.value = event.enabled

            }

            is SpeedometerEvent.ChangeStartButtonText -> {
                _startButtonText.value = event.text
            }


        }
    }


    private fun getTopSpeed(currentSpeed: Int) {
        _topSpeed.value?.let {
            if (currentSpeed > it) {
                _topSpeed.value = currentSpeed
            }
        }

    }
}