package dev.jesselima.geofences

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import dev.jesselima.geofences.databinding.ActivityMainBinding

/**
 * The Treasure Hunt app is a single-player game based on geofences.
 *
 * This app demonstrates how to create and remove geofences using the GeofencingApi. Uses an
 * BroadcastReceiver to monitor geofence transitions and creates notification and finishes the game
 * when the user enters the final geofence (destination).
 *
 * This app requires a device's Location settings to be turned on. It also requires
 * the ACCESS_FINE_LOCATION permission and user consent. For geofences to work
 * in Android Q, app also needs the ACCESS_BACKGROUND_LOCATION permission and user consent.
 */

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val ACTION_GEOFENCE_EVENT = "MainActivity.geofences.action.ACTION_GEOFENCE_EVENT"
private const val PENDING_INTENT_REQUEST_CODE = 0

class HuntMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GeofenceViewModel

    private lateinit var geofenceClient: GeofencingClient

    private val tag = HuntMainActivity::class.java.simpleName

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            this,
            PENDING_INTENT_REQUEST_CODE,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            when {
                isAndroidOsEqualsOrGreaterThan(osVersion = Build.VERSION_CODES.M) -> {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                }
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        viewModel = ViewModelProvider(
            this,
            SavedStateViewModelFactory(
                this.application,
                this
            )
        ).get(GeofenceViewModel::class.java)

        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        geofenceClient = LocationServices.getGeofencingClient(this)

        // Create channel for notifications
        createChannel(this)
    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofence()
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndStartGeofence() {
        if (viewModel.geofenceIsActive()) return
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            isAndroidOsEqualsOrGreaterThan(Build.VERSION_CODES.Q) -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(tag, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            this@HuntMainActivity,
            permissionsArray,
            resultCode
        )
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved() : Boolean {
        val foregroundLocationApproved = (PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                )

        val backgroundPermissionApproved =
            if (isAndroidOsEqualsOrGreaterThan(Build.VERSION_CODES.Q)) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(tag, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            isResultDenied(grantResults[LOCATION_PERMISSION_INDEX]) ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
            isResultDenied(grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX])))
        {
            // Permission denied.
            Snackbar.make(
                binding.activityMapsMain,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkPermissionsAndStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(locationBuilder)

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Prompt the suer to turn on the location
                runCatching {
                    exception.startResolutionForResult(this@HuntMainActivity, REQUEST_TURN_DEVICE_LOCATION_ON)
                }.onFailure { throwable ->
                    Log.d(this@HuntMainActivity::class.java.simpleName, "Error getting location service resolution! ${throwable.message}")
                }
            } else {
                Snackbar.make(
                    binding.activityMapsMain,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsStartGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForClue()
            }
        }
    }

    private fun addGeofenceForClue() {

        if (viewModel.geofenceIsActive()) return

        val currentGeoFenceIndex = viewModel.nextGeofenceIndex()
        if (currentGeoFenceIndex >= GeofencingConstants.getLandMarks().size) {
            removeGeofences()
            viewModel.geofenceActivated()
            return
        }

        val currentGeofenceData = GeofencingConstants.getLandMarks()[currentGeoFenceIndex]

        val geofence = Geofence.Builder()
            .setRequestId(currentGeofenceData.id)
            .setCircularRegion(
                currentGeofenceData.latLong.latitude,
                currentGeofenceData.latLong.longitude,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofenceClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                if (ActivityCompat.checkSelfPermission(
                        this@HuntMainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestForegroundAndBackgroundLocationPermissions()
                } else {
                    geofenceClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
                        addOnSuccessListener {
                            showToast(stringResId = R.string.geofences_added)
                            Log.d("Add Geofence", geofence.requestId)
                            viewModel.geofenceActivated()
                        }
                        addOnFailureListener {
                            showToast(stringResId = R.string.geofences_not_added)
                            if(it.message.isNullOrEmpty().not()) {
                                Log.d(tag, it.message.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        geofenceClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(tag, getString(R.string.geofences_removed))
                Toast.makeText(applicationContext, R.string.geofences_removed, Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                Log.d(tag, getString(R.string.geofences_not_removed))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsStartGeofence(resolve = false)
        }
    }


    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.geofences.action.ACTION_GEOFENCE_EVENT"
    }
}