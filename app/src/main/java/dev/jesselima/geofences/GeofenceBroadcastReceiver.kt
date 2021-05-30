package dev.jesselima.geofences

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

private const val INVALID_INDEX = -1

/**
 *  The broadcast receiver is how Android apps can send or receive broadcast messages from the
 *  Android system and other Android apps
 *
 * Triggered by the Geofence.  Since we only have one active Geofence at once, we pull the request
 * ID from the first Geofence, and locate it within the registered landmark data in our
 * GeofencingConstants within GeofenceUtils, which is a linear string search. If we had  very large
 * numbers of Geofence possibilities, it might make sense to use a different data structure.  We
 * then pass the Geofence index into the notification, which allows us to have a custom "found"
 * message associated with each Geofence.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = GeofenceBroadcastReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "---> onReceive Called!")

        if (intent.action == HuntMainActivity.ACTION_GEOFENCE_EVENT) {

            Log.d(TAG, "---> action is ACTION_GEOFENCE_EVENT!")

            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                val errorMessage = handleGeofenceError(context, geofencingEvent.errorCode)
                Log.d(this::class.java.simpleName, "---> GeofenceEvent has Error: $errorMessage")
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(this::class.java.simpleName, context.getString(R.string.geofence_entered))
                val fenceId = when {
                    geofencingEvent.triggeringGeofences.isNotEmpty() -> {
                        geofencingEvent.triggeringGeofences.first().requestId
                    }
                    else -> {
                        Log.d(
                            this::class.java.simpleName,
                            "---> No Geofence trigger found! Houston! We have a situation."
                        )
                        return
                    }
                }

                val foundGeofenceIndex = GeofencingConstants.getLandMarks().indexOfFirst {
                    it.id == fenceId
                }

                if (INVALID_INDEX == foundGeofenceIndex) {
                    Log.d(this::class.java.simpleName, "---> Unknown Geofence: Errrrrooooou!")
                    return
                }

                val notificationManager = ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

                notificationManager.sendGeofenceEnteredNotification(context, foundGeofenceIndex)
            }

        }

    }
}

