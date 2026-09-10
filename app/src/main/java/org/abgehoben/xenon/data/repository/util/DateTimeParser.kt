package org.abgehoben.xenon.data.repository.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeParser {
    val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val GERMAN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    //TODO: I do not like that this is language dependant
    val GERMAN_DAYS_MAP: Map<Int, String> = mapOf(
        1 to "Montag",
        2 to "Dienstag",
        3 to "Mittwoch",
        4 to "Donnerstag",
        5 to "Freitag"
    )

    fun parseDateFlexible(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            if (dateStr.contains("-")) {
                LocalDate.parse(dateStr.take(10), ISO_DATE_FORMATTER)
            } else if (dateStr.contains(".")) {
                LocalDate.parse(dateStr.take(10), GERMAN_DATE_FORMATTER)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseIsoLocal(dtStr: String?): LocalDateTime? {
        if (dtStr.isNullOrBlank()) return null
        val clean = dtStr.replace("Z", "")
        return try {
            if (clean.length >= 19) LocalDateTime.parse(clean.substring(0, 19))
            else if (clean.contains("T")) LocalDateTime.parse(clean)
            else LocalDate.parse(clean.substring(0, 10)).atStartOfDay()
        } catch (_: Exception) {
            try {
                LocalDate.parse(clean.substring(0, 10)).atStartOfDay()
            } catch (_: Exception) {
                null
            }
        }
    }
}