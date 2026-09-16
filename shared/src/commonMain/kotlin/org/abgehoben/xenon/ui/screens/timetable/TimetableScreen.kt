package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.ui.components.LoadingView
import org.abgehoben.xenon.ui.components.SyncErrorState
import org.abgehoben.xenon.ui.screens.timetable.components.TimetableTopBar
import org.abgehoben.xenon.ui.screens.timetable.sheets.LessonDetailsBottomSheet
import org.abgehoben.xenon.ui.screens.timetable.views.DailyListView
import org.abgehoben.xenon.ui.screens.timetable.views.TimetableWeeklyGrid
import org.abgehoben.xenon.ui.theme.Dimens
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate

@Composable
fun TimetableRoute(
    viewModel: TimetableViewModel = koinViewModel()
) {
    val timetableGrid by viewModel.timetableGrid.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val viewMode by viewModel.timetableViewMode.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    TimetableScreen(
        grid = timetableGrid,
        syncError = syncError,
        isRefreshing = isRefreshing,
        viewMode = viewMode,
        userSettings = userSettings,
        onPrevWeek = viewModel::prevWeek,
        onNextWeek = viewModel::nextWeek,
        onRefresh = { viewModel.refreshData(forceRefresh = true) },
        onViewModeChange = viewModel::setTimetableViewMode
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(
    grid: TimetableGrid?,
    syncError: String?,
    isRefreshing: Boolean,
    viewMode: TimetableViewMode,
    userSettings: UserSettings,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRefresh: () -> Unit,
    onViewModeChange: (TimetableViewMode) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedSlot by remember { mutableStateOf<Triple<Int, MergedSlot, TimetableSlot>?>(null) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    val initialPage = remember {
        val today = LocalDate.now().dayOfWeek.value
        if (today in 1..5) today - 1 else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 5 })
    val pullToRefreshState = rememberPullToRefreshState()

    val calWeek = grid?.calWeek?.toString() ?: ""
    val weekType = grid?.weekType ?: ""
    val mondayDate = grid?.mondayDate ?: LocalDate.now()

    Scaffold(
        contentWindowInsets = WindowInsets(
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone
        ),
        topBar = {
            TimetableTopBar(
                calWeek = calWeek,
                weekType = weekType,
                viewMode = viewMode,
                onPrevWeek = onPrevWeek,
                onNextWeek = onNextWeek,
                onViewModeChange = onViewModeChange
            )
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
            if (grid == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (syncError != null) {
                        SyncErrorState(error = syncError, onRetry = onRefresh)
                    } else {
                        LoadingView()
                    }
                }
            } else {
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ViewModeTransition"
                ) { mode ->
                    when (mode) {
                        TimetableViewMode.WEEKLY -> {
                            TimetableWeeklyGrid(
                                grid = grid,
                                mergeLessons = userSettings.mergeLessons,
                                scaleBreaks = userSettings.scaleBreaks,
                                onSlotClick = { d, merged, slot -> selectedSlot = Triple(d, merged, slot) },
                                onDayClick = { dayOffset ->
                                    scope.launch { pagerState.scrollToPage(dayOffset) }
                                    onViewModeChange(TimetableViewMode.DAILY)
                                }
                            )
                        }
                        TimetableViewMode.DAILY -> {
                            DailyListView(
                                grid = grid,
                                pagerState = pagerState,
                                monday = mondayDate,
                                mergeLessons = userSettings.mergeLessons,
                                scaleBreaks = userSettings.scaleBreaks,
                                onTabSelected = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                                onSlotClick = { d, merged, slot -> selectedSlot = Triple(d, merged, slot) }
                            )
                        }
                    }
                }
            }
        }

        if (selectedSlot != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedSlot = null },
                sheetState = sheetState,
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = Dimens.RadiusDialog, topEnd = Dimens.RadiusDialog)
            ) {
                val (d, merged, slot) = selectedSlot!!
                LessonDetailsBottomSheet(
                    dayIndex = d,
                    mergedSlot = merged,
                    slot = slot,
                    classHours = grid?.classHours ?: emptyList(),
                    mondayDate = mondayDate,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            selectedSlot = null
                        }
                    }
                )
            }
        }
    }
}