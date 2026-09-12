package org.abgehoben.xenon.util

import android.content.Context
import org.abgehoben.xenon.R
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorFormatter {
    fun format(context: Context, e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") || msg.contains("Session expired") -> context.getString(R.string.error_session_expired)
            msg.contains("429") || msg.contains("Rate limit") -> context.getString(R.string.error_rate_limited)
            e is UnknownHostException || e.cause is UnknownHostException -> context.getString(R.string.error_no_internet)
            e is ConnectException || e.cause is ConnectException -> context.getString(R.string.error_server_unreachable)
            e is SocketTimeoutException || e.cause is SocketTimeoutException -> context.getString(R.string.error_timeout)
            !e.localizedMessage.isNullOrEmpty() -> e.localizedMessage!!
            else -> context.getString(R.string.error_network_generic)
        }
    }
}