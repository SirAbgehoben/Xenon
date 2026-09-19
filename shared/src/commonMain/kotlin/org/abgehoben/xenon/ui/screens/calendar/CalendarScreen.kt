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
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.ui.components.SyncErrorState
import org.abgehoben.xenon.ui.screens.calendar.components.CalendarGrid
import org.abgehoben.xenon.ui.screens.calendar.components.EventListItem
import org.abgehoben.xenon.ui.screens.calendar.components.MonthSelector
import org.abgehoben.xenon.ui.screens.calendar.sheets.EventDetailsBottomSheet
import org.abgehoben.xenon.ui.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.util.*
import org.abgehoben.xenon.util.YearMonth

@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel = koinViewModel()
) {
    val eventsByDay by viewModel.calendarEvents.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    CalendarScreen(
        eventsByDay = eventsByDay,
        syncError = syncError,
        isSyncing = isSyncing,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData(forceRefresh = true) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalendarScreen(
    eventsByDay: Map<LocalDate, List<ProcessedEvent>>,
    syncError: String?,
    isSyncing: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    var selectedEvent by remember { mutableStateOf<ProcessedEvent?>(null) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        contentWindowInsets = WindowInsets(
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone
        ),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                MonthSelector(
                    currentMonth = currentMonth,
                    onMonthChange = { newMonth ->
                        currentMonth = newMonth
                        if (selectedDate.year != newMonth.year || selectedDate.month != newMonth.month) {
                            val clampedDay = selectedDate.day.coerceAtMost(newMonth.lengthOfMonth())
                            selectedDate = newMonth.atDay(clampedDay)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
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
                    SyncErrorState(error = syncError, onRetry = onRefresh)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Dimens.SpacingJumbo)
                ) {
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
                            modifier = Modifier.padding(
                                top = Dimens.SpacingMedium,
                                bottom = Dimens.SpacingSmall
                            ),
                            thickness = Dimens.StrokeThin,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }

                    val events = eventsByDay[selectedDate].orEmpty()
                    if (events.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.SpacingJumbo),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.no_events_today),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        items(events) { event ->
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.SpacingNormal,
                                    vertical = Dimens.SpacingExtraSmall
                                )
                            ) {
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
                shape = RoundedCornerShape(
                    topStart = Dimens.RadiusDialog,
                    topEnd = Dimens.RadiusDialog
                )
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