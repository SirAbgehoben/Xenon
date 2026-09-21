package org.abgehoben.xenon.platform

class WasmPlatformNotifier : PlatformNotifier {
    override fun showToast(message: String) {
        println("[Web Toast] $message")
    }
}