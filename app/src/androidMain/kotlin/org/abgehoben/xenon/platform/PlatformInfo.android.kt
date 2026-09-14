package org.abgehoben.xenon.platform

import android.os.Build

class AndroidPlatformInfo : PlatformInfo {
    override val manufacturer: String = Build.MANUFACTURER
    override val model: String = Build.MODEL
    override val osVersion: String = Build.VERSION.RELEASE
    override val apiLevel: Int = Build.VERSION.SDK_INT
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()