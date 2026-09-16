package org.abgehoben.xenon.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusExtraSmall),
    small = RoundedCornerShape(Dimens.RadiusMedium),
    medium = RoundedCornerShape(Dimens.RadiusLarge),
    large = RoundedCornerShape(Dimens.RadiusDialog),
    extraLarge = RoundedCornerShape(Dimens.RadiusExtraLarge)
)