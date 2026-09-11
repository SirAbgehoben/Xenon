package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun LiveTimeIndicatorOverlay(
    yOffset: Dp,
    currentDayIndex: Int,
    colWidth: Dp,
    blockGap: Dp,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val totalDays = 5

    val yOffsetPx = with(density) { yOffset.toPx() }
    val colWidthPx = with(density) { colWidth.toPx() }
    val blockGapPx = with(density) { blockGap.toPx() }
    val totalWidthPx = (colWidthPx * totalDays) + (blockGapPx * (totalDays - 1))

    Canvas(modifier = modifier) {
        drawLine(
            color = lineColor.copy(alpha = 0.35f),
            start = Offset(0f, yOffsetPx),
            end = Offset(totalWidthPx, yOffsetPx),
            strokeWidth = Dimens.StrokeMedium.toPx()
        )

        if (currentDayIndex in 1..totalDays) {
            val dayLeft = (colWidthPx + blockGapPx) * (currentDayIndex - 1)
            val dayRight = dayLeft + colWidthPx

            drawLine(
                color = lineColor,
                start = Offset(dayLeft, yOffsetPx),
                end = Offset(dayRight, yOffsetPx),
                strokeWidth = Dimens.TimeIndicatorActiveHeight.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}