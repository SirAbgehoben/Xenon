package org.abgehoben.xenon.platform

interface PlatformInfo {
    val manufacturer: String
    val model: String
    val osVersion: String
    val apiLevel: Int
}

expect fun getPlatformInfo(): PlatformInfo