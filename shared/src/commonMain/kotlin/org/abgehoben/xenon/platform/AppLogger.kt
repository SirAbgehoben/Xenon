package org.abgehoben.xenon.platform

object AppLogger {
    fun d(tag: String, message: String) {
        println("[$tag] DEBUG: $message")
    }
    fun w(tag: String, message: String) {
        println("[$tag] WARN: $message")
    }
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}