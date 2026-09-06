package org.abgehoben.xenon.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.ProcessedEvent
import org.abgehoben.xenon.ui.components.SyncErrorState
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val eventsByDay by viewModel.calendarEvents.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    var selectedEvent by remember { mutableStateOf<ProcessedEvent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData(forceRefresh = true) },
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (eventsByDay.isEmpty() && syncError != null && !isSyncing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SyncErrorState(error = syncError!!, onRetry = { viewModel.refreshData() })
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
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }

                    val events = eventsByDay[selectedDate] ?: emptyList()
                    if (events.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_events_today),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(events) { event ->
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
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
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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