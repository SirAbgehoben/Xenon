package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun TimetableTopBar(
    calWeek: String,
    weekType: String,
    viewMode: TimetableViewMode,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onViewModeChange: (TimetableViewMode) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar Week Navigator Pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = Dimens.ElevationLevel2,
                modifier = Modifier.height(Dimens.HeaderPillHeight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Dimens.PillPadding)
                ) {
                    IconButton(
                        onClick = onPrevWeek,
                        modifier = Modifier.size(Dimens.PillIndicatorSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(Res.string.cd_prev_week),
                            modifier = Modifier.size(Dimens.ActionIconSize)
                        )
                    }

                    Text(
                        text = stringResource(Res.string.calendar_week_format, calWeek, weekType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = Dimens.SpacingStandard),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(Dimens.PillIndicatorSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(Res.string.cd_next_week),
                            modifier = Modifier.size(Dimens.ActionIconSize)
                        )
                    }
                }
            }

            // View Mode Selector Pill (Grid vs List)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = Dimens.ElevationLevel2,
                modifier = Modifier.height(Dimens.HeaderPillHeight)
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.PillPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gridColor by animateColorAsState(
                        targetValue = if (viewMode == TimetableViewMode.WEEKLY) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "gridColor"
                    )
                    val gridIconColor by animateColorAsState(
                        targetValue = if (viewMode == TimetableViewMode.WEEKLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "gridIconColor"
                    )
                    val listColor by animateColorAsState(
                        targetValue = if (viewMode != TimetableViewMode.WEEKLY) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "listColor"
                    )
                    val listIconColor by animateColorAsState(
                        targetValue = if (viewMode != TimetableViewMode.WEEKLY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "listIconColor"
                    )

                    Box(
                        modifier = Modifier
                            .size(Dimens.PillIndicatorSize)
                            .clip(CircleShape)
                            .background(gridColor)
                            .clickable { onViewModeChange(TimetableViewMode.WEEKLY) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = stringResource(Res.string.cd_week_view),
                            tint = gridIconColor,
                            modifier = Modifier.size(Dimens.BottomBarIconSize)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(Dimens.PillIndicatorSize)
                            .clip(CircleShape)
                            .background(listColor)
                            .clickable { onViewModeChange(TimetableViewMode.DAILY) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = stringResource(Res.string.cd_day_view),
                            tint = listIconColor,
                            modifier = Modifier.size(Dimens.BottomBarIconSize)
                        )
                    }
                }
            }
        }
    }
}