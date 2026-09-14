package org.abgehoben.xenon.data.repository.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeParser {
    val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val GERMAN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parseDateFlexible(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val clean = dateStr.trim().take(10)
            if (clean.contains("-")) {
                LocalDate.parse(clean, ISO_DATE_FORMATTER)
            } else if (clean.contains(".")) {
                LocalDate.parse(clean, GERMAN_DATE_FORMATTER)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseIsoLocal(dtStr: String?): LocalDateTime? {
        if (dtStr.isNullOrBlank()) return null
        val clean = dtStr.replace("Z", "")
        return try {
            if (clean.length >= 19) {
                LocalDateTime.parse(clean.substring(0, 19))
            } else if (clean.contains("T")) {
                LocalDateTime.parse(clean)
            } else {
                LocalDate.parse(clean.substring(0, 10)).atStartOfDay()
            }
        } catch (_: Exception) {
            try {
                LocalDate.parse(clean.substring(0, 10)).atStartOfDay()
            } catch (_: Exception) {
                null
            }
        }
    }
}