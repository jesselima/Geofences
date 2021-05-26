package dev.jesselima.geofences

import android.os.Build

fun isAndroidOsEqualsOrGreaterThan(osVersion: Int): Boolean {
    return Build.VERSION.SDK_INT >= osVersion
}