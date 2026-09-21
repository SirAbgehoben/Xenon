package org.abgehoben.xenon.ui.screens.settings.util

import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.data.model.auth.UserRole
import org.jetbrains.compose.resources.StringResource

val UserRole.labelRes: StringResource
    get() = when (this) {
        UserRole.STUDENT -> Res.string.user_role_student
        UserRole.PARENT -> Res.string.user_role_parent
        UserRole.TEACHER -> Res.string.user_role_teacher
    }