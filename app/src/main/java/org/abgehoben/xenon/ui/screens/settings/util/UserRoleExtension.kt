package org.abgehoben.xenon.ui.screens.settings.util

import androidx.annotation.StringRes
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.auth.UserRole

val UserRole.labelRes: Int
    @StringRes get() = when (this) {
        UserRole.STUDENT -> R.string.user_role_student
        UserRole.PARENT -> R.string.user_role_parent
        UserRole.TEACHER -> R.string.user_role_teacher
    }