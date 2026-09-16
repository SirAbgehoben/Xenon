package org.abgehoben.xenon.util

object ErrorFormatter {
    fun format(e: Throwable): String {
        val msg = e.message ?: ""
        val name = e::class.simpleName ?: ""
        return when {
            msg.contains("401") || msg.contains("Session expired") -> "Ihre Sitzung ist abgelaufen. Bitte erneut anmelden."
            msg.contains("429") || msg.contains("Rate limit") -> "Zu viele Anfragen. Bitte warten Sie kurz."
            name.contains("UnknownHost") || msg.contains("UnknownHost") -> "Keine Internetverbindung verfügbar."
            name.contains("ConnectException") || msg.contains("ConnectException") -> "Server konnte nicht erreicht werden."
            name.contains("Timeout") || msg.contains("Timeout") -> "Zeitüberschreitung bei der Anfrage."
            !e.message.isNullOrEmpty() -> e.message!!
            else -> "Ein unerwarteter Netzwerkfehler ist aufgetreten."
        }
    }
}