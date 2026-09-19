package org.abgehoben.xenon.platform

import kotlinx.browser.window
import org.abgehoben.xenon.platform.PlatformInfo

class WasmPlatformInfo : PlatformInfo {
    override val manufacturer: String = "Web"
    override val model: String = runCatching { window.navigator.userAgent.take(32) }.getOrDefault("Browser")
    override val osVersion: String = "WebAssembly"
    override val apiLevel: Int = 0
}