package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.abgehoben.xenon.R

@Composable
fun TimetableTopBar(
    calWeek: String,
    weekType: String,
    isWeeklyView: Boolean,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToggleViewMode: () -> Unit
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onPrevWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.cd_prev_week),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.calendar_week_format, calWeek, weekType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.cd_next_week),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gridColor by animateColorAsState(if (isWeeklyView) MaterialTheme.colorScheme.primary else Color.Transparent, label = "gridColor")
                    val gridIconColor by animateColorAsState(if (isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "gridIconColor")
                    val listColor by animateColorAsState(if (!isWeeklyView) MaterialTheme.colorScheme.primary else Color.Transparent, label = "listColor")
                    val listIconColor by animateColorAsState(if (!isWeeklyView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "listIconColor")

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(gridColor)
                            .clickable { if (!isWeeklyView) onToggleViewMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = stringResource(R.string.cd_week_view),
                            tint = gridIconColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(listColor)
                            .clickable { if (isWeeklyView) onToggleViewMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = stringResource(R.string.cd_day_view),
                            tint = listIconColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}