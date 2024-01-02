package co.paulfran.speedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import co.paulfran.speedometer.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import timber.log.Timber
import kotlin.math.roundToInt


const val METER_TO_KILOMETER = 1000
const val LOCATION_REQUEST_CODE = 44

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProvideClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val timeDelayInMillis = 4000
    private var autoPauseTime = 0L;
    private var autoResumeTime = 0L;
    private val viewModel: SpeedometerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        //binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView(binding.root)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        subscribeToObservables()



        fusedLocationProvideClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {


                locationResult.lastLocation?.let { userLocation: Location ->

                    val speedKph: Float = userLocation.speed * 3600 / 1000

                    val speed = speedKph.toInt()
                    if (speed <= 0) {
                        autoPause()
                    } else {
                        autoResume()
                    }


                    val isPaused = viewModel.isPaused.value
                    //if not paused then update the speed
                    if (isPaused!!.not()) {

                        viewModel.onEvent(event = SpeedometerEvent.ChangeSpeed(speed = speed.toDouble()))
                        viewModel.onEvent(
                            event = SpeedometerEvent.ChangeLocation(
                                latitude = userLocation.latitude,
                                longitude = userLocation.longitude))
                    }
                }


            }


        }



        binding.startButton.setOnClickListener {
            if (viewModel.isPaused.value!!) {
                viewModel.onEvent(SpeedometerEvent.ResumeTracking)
                return@setOnClickListener
            }
            viewModel.onEvent(SpeedometerEvent.StartTracking)

        }

        binding.pauseButton.setOnClickListener {
            viewModel.onEvent(event = SpeedometerEvent.PauseTracking)
        }

        binding.stopButton.setOnClickListener {
            viewModel.onEvent(event = SpeedometerEvent.StopTracking)
        }
    }

    //wait at least for 4 seconds before auto pausing
    private fun autoPause() {
        val isWaitedPause = (autoPauseTime + timeDelayInMillis < System.currentTimeMillis())
        //if waited for at least 4 seconds then pause
        if (isWaitedPause && viewModel.isPaused.value!!.not()) {
            viewModel.onEvent(SpeedometerEvent.PauseTracking)
            return
        }
        autoPauseTime = System.currentTimeMillis()
    }

    //wait at least for at least 4 seconds before auto resuming
    private fun autoResume() {

        val isWaitedResume = (autoResumeTime + timeDelayInMillis < System.currentTimeMillis())
        //if waited for 4 seconds and is paused then resume
        if (isWaitedResume && viewModel.isPaused.value!! && viewModel.isResumed.value!!.not()) {
            viewModel.onEvent(SpeedometerEvent.ResumeTracking)
            return
        }
        autoResumeTime = System.currentTimeMillis()
    }

    //subscribe to changes from viewmodel live data
    private fun subscribeToObservables() {

        with(viewModel) {

            isTracking.observe(this@MainActivity) { isTracking ->
                if (isTracking) {
                    startLocationUpdates()
                    return@observe
                }
                stopLocationUpdates()
            }

            travelledDistance.observe(this@MainActivity) {
                val travelledDistanceVisibility = if (it.isEmpty()) View.INVISIBLE else View.VISIBLE
                binding.travelledDistance.visibility = travelledDistanceVisibility
                binding.tvTravelledDistance.visibility = travelledDistanceVisibility


                binding.travelledDistance.text = it


                val temp_time = 20.00

                // val temp_It = println(temp_intIt.replace('m',' '))

                val temp_itStr = it.replace(" m", "").replace(" km", "")


                if (temp_itStr == "") {
                    Log.v("if_yow", temp_itStr.toString())
                } else {
                    val temp_calIt = temp_itStr.toFloat()
                    val tempAvgSpd = temp_calIt / temp_time
                    val temp_weight = 70

                    val roundoff = (tempAvgSpd * 10.0).roundToInt() / 10.0

                    val kcal = temp_time * (7.5 * 3.5 * temp_weight) / 200

                    Log.v("else_yow", roundoff.toString())
                    binding.avgSpeed.text = roundoff.toString()
                    binding.kcal.text = kcal.toString()
                }


                //average speed result
            }

            topSpeed.observe(this@MainActivity) {
                binding.topSpeed.text = it.toString()
            }

            currentSpeedKph.observe(this@MainActivity) {
                binding.speed.text = it.roundToInt().toString()
            }

            currentLocation.observe(this@MainActivity) {
                binding.latitude.text = it.latitude.toString()
                binding.longitude.text = it.longitude.toString()
            }

            stopButtonEnabled.observe(this@MainActivity) {
                binding.stopButton.isEnabled = it
            }

            startButtonEnabled.observe(this@MainActivity) {
                binding.startButton.isEnabled = it
            }

            pauseButtonEnabled.observe(this@MainActivity) {
                binding.pauseButton.isEnabled = it
            }

            startButtonText.observe(this@MainActivity) {
                binding.startButton.text = it
            }
        }
    }


    //remove observers to avoid memory leaks
    private fun unsubscribeToObservables() {
        with(viewModel) {
            isTracking.removeObservers(this@MainActivity)
            travelledDistance.removeObservers(this@MainActivity)
            topSpeed.removeObservers(this@MainActivity)
            currentSpeedKph.removeObservers(this@MainActivity)
            currentLocation.removeObservers(this@MainActivity)
            stopButtonEnabled.removeObservers(this@MainActivity)
            startButtonEnabled.removeObservers(this@MainActivity)
            pauseButtonEnabled.removeObservers(this@MainActivity)
            startButtonText.removeObservers(this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        unsubscribeToObservables()
    }


    private fun startLocationUpdates() {
        if (!permissionsGranted()) {
            requestPermissions()
            return
        }

        if (isLocationEnabled()) {
            requestNewLocationData()
            return
        }

        Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)

    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            smallestDisplacement = 1f
        }


        fusedLocationProvideClient.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.myLooper()
        )

    }

    private fun stopLocationUpdates() {
        fusedLocationProvideClient.removeLocationUpdates(locationCallback)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates()
            }
        }
    }


    private fun permissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
               && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), LOCATION_REQUEST_CODE

        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

}

//to format the distance
fun Double.formatToDistance(): String {
    return if (this < METER_TO_KILOMETER) {
        "${"%.2f".format(this)} m"
    } else {
        "${"%.2f".format(this / METER_TO_KILOMETER)} km"
    }
}
