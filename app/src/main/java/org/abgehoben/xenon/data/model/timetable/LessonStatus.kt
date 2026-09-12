package org.abgehoben.xenon.data.model.timetable

import kotlinx.serialization.Serializable

@Serializable
enum class LessonStatus { //TODO
    REGULAR,
    SUBSTITUTION,
    CANCELLED,
    HOLIDAY,
    FREE_PERIOD
}