package org.abgehoben.xenon.platform

import org.abgehoben.xenon.XenonApplication

actual fun producePath(fileName: String): String {
    val context = XenonApplication.instance
        ?: throw IllegalStateException("Android Context not initialized")
    return context.filesDir.resolve(fileName).absolutePath
}