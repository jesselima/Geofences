package dev.jesselima.geofences

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GeofenceViewModel

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

        // Create channel for notifications
        createChannel(this)
        requestForegroundAndBackgroundLocationPermissions()
    }

    @TargetApi(29)
    fun requestForegroundAndBackgroundLocationPermissions() {
        if (isForegroundAndBackgroundPermissionsGranted().not()) {

            var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

            val resultCode = when {
                isAndroidOsEqualsOrGreaterThan(Build.VERSION_CODES.O) -> {
                    permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
                }
                else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            }

            ActivityCompat.requestPermissions(
                this@MainActivity,
                permissionArray,
                resultCode
            )
        }
    }

    @TargetApi(29)
    private fun isForegroundAndBackgroundPermissionsGranted() : Boolean {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                if(isAndroidOsEqualsOrGreaterThan(Build.VERSION_CODES.Q)) {
                    isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    true
                }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            (grantResults.isEmpty() || isPermissionResultDenied(grantResults[LOCATION_PERMISSION_INDEX])) ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE) && grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                binding.activityMapsMain,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            // Permission have been granted. Then we can start geofence.
            checkDeviceLocationSettingsStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsStartGeofence() {
        // TODO("Not yet implemented")
    }

    private fun isPermissionResultDenied(grantedResult: Int) : Boolean {
        return grantedResult == PackageManager.PERMISSION_DENIED
    }


}