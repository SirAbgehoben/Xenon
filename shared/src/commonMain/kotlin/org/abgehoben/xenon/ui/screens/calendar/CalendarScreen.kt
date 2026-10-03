package org.abgehoben.xenon.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.platform.PlatformDateFormatter
import org.abgehoben.xenon.ui.components.SyncErrorState
import org.abgehoben.xenon.ui.screens.calendar.components.CalendarGrid
import org.abgehoben.xenon.ui.screens.calendar.components.EventListItem
import org.abgehoben.xenon.ui.screens.calendar.components.MonthSelector
import org.abgehoben.xenon.ui.screens.calendar.sheets.EventDetailsBottomSheet
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.util.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.no_events_today

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
        // Automatically clears the status bar, camera cutouts, and side nav bar
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
        )
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 600.dp

                    if (isWide) {
                        // ADAPTIVE SPLIT VIEW (Landscape)
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Month Selector + Calendar Grid
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 380.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = Dimens.SpacingSmall),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MonthSelector(
                                    currentMonth = currentMonth,
                                    onMonthChange = { newMonth ->
                                        currentMonth = newMonth
                                        val clampedDay = selectedDate.day.coerceAtMost(newMonth.lengthOfMonth())
                                        selectedDate = newMonth.atDay(clampedDay)
                                    }
                                )
                                CalendarGrid(
                                    currentMonth = currentMonth,
                                    selectedDate = selectedDate,
                                    eventsByDay = eventsByDay,
                                    onDateSelected = { selectedDate = it }
                                )
                            }

                            VerticalDivider(
                                thickness = Dimens.StrokeThin,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            // Right Pane: Events for selected day
                            val events = eventsByDay[selectedDate].orEmpty()
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentPadding = PaddingValues(
                                    start = Dimens.SpacingLarge,
                                    end = Dimens.SpacingLarge,
                                    top = Dimens.SpacingLarge,
                                    bottom = Dimens.SpacingJumbo
                                )
                            ) {
                                item {
                                    Text(
                                        text = PlatformDateFormatter.formatEventDate(selectedDate),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = Dimens.SpacingMedium)
                                    )
                                }

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
                                        Box(modifier = Modifier.padding(vertical = Dimens.SpacingExtraSmall)) {
                                            EventListItem(event = event, onClick = { selectedEvent = event })
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        //portrait view
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = Dimens.SpacingJumbo)
                        ) {
                            item {
                                MonthSelector(
                                    currentMonth = currentMonth,
                                    onMonthChange = { newMonth ->
                                        currentMonth = newMonth
                                        val clampedDay = selectedDate.day.coerceAtMost(newMonth.lengthOfMonth())
                                        selectedDate = newMonth.atDay(clampedDay)
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