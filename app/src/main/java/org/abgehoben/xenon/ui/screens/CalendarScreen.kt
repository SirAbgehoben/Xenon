package org.abgehoben.xenon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.launch
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.data.ProcessedEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val eventsByDay by viewModel.calendarEvents.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    var selectedEvent by remember { mutableStateOf<ProcessedEvent?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalender", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData(forceRefresh = true) },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (eventsByDay.isEmpty() && syncError != null && !isSyncing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Synchronisierung fehlgeschlagen", style = MaterialTheme.typography.titleMedium)
                        Text(syncError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        MonthSelector(
                            currentMonth = currentMonth,
                            onMonthChange = { newMonth ->
                                currentMonth = newMonth
                                // Keep selectedDate in sync with the viewed month
                                if (selectedDate.year != newMonth.year || selectedDate.month != newMonth.month) {
                                    val clampedDay = selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth())
                                    selectedDate = newMonth.atDay(clampedDay)
                                }
                            }
                        )
                    }

                    item {
                        CalendarGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            eventsByDay = eventsByDay,
                            onDateSelected = { selectedDate = it }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    val events = eventsByDay[selectedDate] ?: emptyList()
                    if (events.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Keine Termine für diesen Tag.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(events) { event ->
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                EventListItem(event = event, onClick = { selectedEvent = event })
                            }
                        }
                    }
                }
            }
        }

        if (selectedEvent != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedEvent = null },
                sheetState = sheetState,
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                EventDetailsBottomSheet(
                    event = selectedEvent!!,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            selectedEvent = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EventDetailsBottomSheet(
    event: ProcessedEvent,
    onDismiss: () -> Unit
) {
    val accentColor = if (event.isHoliday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Schließen")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = event.startDate.format(DateTimeFormatter.ofPattern("E | dd.MM.yy", Locale.GERMAN)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (event.allDay) "Ganztägig" else "${event.startTime} - ${event.endTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "Optionen")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accentColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.category,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (event.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = event.location, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (event.organizer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = event.organizer, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Clean HTML formatting and entities
                    val cleanText = remember(event.description) {
                        HtmlCompat.fromHtml(event.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                    }
                    Text(text = cleanText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Voriger Monat")
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat")
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsByDay: Map<LocalDate, List<ProcessedEvent>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val startPadding = currentMonth.atDay(1).dayOfWeek.value - 1
    val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val totalCells = daysInMonth + startPadding
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startPadding + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val dayEvents = eventsByDay[date] ?: emptyList()
                        val hasHoliday = dayEvents.any { it.isHoliday }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (dayEvents.isNotEmpty() && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasHoliday) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.secondary
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun EventListItem(event: ProcessedEvent, onClick: () -> Unit) {
    val accentColor = if (event.isHoliday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val bgColor = if (event.isHoliday) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (event.allDay) "Ganztägig" else event.startTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}