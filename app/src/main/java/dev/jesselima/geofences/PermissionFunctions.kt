package dev.jesselima.geofences

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

fun Context.isPermissionGranted(permission: String): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            permission
        )
    }

fun isAndroidOsEqualsOrGreaterThan(osVersion: Int): Boolean {
    return Build.VERSION.SDK_INT >= osVersion
}