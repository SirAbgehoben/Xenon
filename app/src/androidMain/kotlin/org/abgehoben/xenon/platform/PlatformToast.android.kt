package org.abgehoben.xenon.platform

import android.content.Context
import android.widget.Toast

class AndroidPlatformNotifier(private val context: Context) : PlatformNotifier {
    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}