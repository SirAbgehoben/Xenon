package org.abgehoben.xenon.platform

import android.widget.Toast
import org.abgehoben.xenon.XenonApplication

actual fun showToast(message: String) {
    XenonApplication.instance?.let { context ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}