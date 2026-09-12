package org.abgehoben.xenon.data.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT,
    PARENT,
    TEACHER;

    companion object {
        fun fromString(value: String?): UserRole {
            val clean = value?.lowercase()?.removePrefix("role_")?.trim() ?: return STUDENT
            return when (clean) {
                "student", "schueler", "schüler", "pupil" -> STUDENT
                "parent", "eltern", "elternteil" -> PARENT
                "teacher", "lehrer", "lehrkraft", "staff" -> TEACHER
                else -> STUDENT
            }
        }
    }
}