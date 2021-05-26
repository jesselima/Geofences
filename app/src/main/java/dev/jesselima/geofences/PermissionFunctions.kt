package dev.jesselima.geofences

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun Context.isPermissionGranted(permission: String): Boolean {
        return  ActivityCompat.checkSelfPermission(
            this,
            permission
        ) != PackageManager.PERMISSION_GRANTED
}

fun isDenied(grantedResult: Int) : Boolean {
    return grantedResult != PackageManager.PERMISSION_GRANTED
}

fun isGranted(grantedResult: Int) : Boolean {
    return grantedResult == PackageManager.PERMISSION_GRANTED
}