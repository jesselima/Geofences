package dev.jesselima.geofences

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun Context.isPermissionGranted(permission: String): Boolean {
    return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
        this,
        permission
    )
}

fun isResultDenied(grantedResult: Int) : Boolean {
    return grantedResult == PackageManager.PERMISSION_DENIED
}

fun isResultGranted(grantedResult: Int) : Boolean {
    return grantedResult == PackageManager.PERMISSION_GRANTED
}