package org.abgehoben.xenon.data.repository.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.abgehoben.xenon.util.atStartOfDay

object DateTimeParser {
    fun parseDateFlexible(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val clean = dateStr.trim().take(10)
            if (clean.contains("-")) {
                LocalDate.parse(clean)
            } else if (clean.contains(".")) {
                val parts = clean.split(".")
                if (parts.size >= 3) {
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val year = parts[2].toInt()
                    LocalDate(year, month, day)
                } else null
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

    fun formatDateGerman(date: LocalDate): String {
        val d = date.dayOfMonth.toString().padStart(2, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        return "$d.$m.${date.year}"
    }
}