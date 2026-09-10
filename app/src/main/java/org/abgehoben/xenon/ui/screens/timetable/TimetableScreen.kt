package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.components.LoadingView
import org.abgehoben.xenon.ui.components.SyncErrorState
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(viewModel: MainViewModel) {
    val timetableGrid by viewModel.timetableGrid.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isWeeklyView by viewModel.isWeeklyView.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedSlot by remember { mutableStateOf<Triple<Int, MergedSlot, TimetableSlot>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialPage = remember {
        val today = LocalDate.now().dayOfWeek.value
        if (today in 1..5) today - 1 else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 5 })
    val pullToRefreshState = rememberPullToRefreshState()

    val calWeek = timetableGrid?.calWeek?.toString() ?: ""
    val weekType = timetableGrid?.weekType ?: ""
    val mondayDate = timetableGrid?.mondayDate ?: LocalDate.now()

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
                isWeeklyView = isWeeklyView,
                onPrevWeek = { viewModel.prevWeek() },
                onNextWeek = { viewModel.nextWeek() },
                onToggleViewMode = { viewModel.toggleViewMode() }
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
            if (timetableGrid == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (syncError != null) {
                        SyncErrorState(error = syncError!!, onRetry = viewModel::refreshData)
                    } else {
                        LoadingView()
                    }
                }
            } else {
                AnimatedContent(
                    targetState = isWeeklyView,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ViewModeTransition"
                ) { weekly ->
                    if (weekly) {
                        UntisWeeklyGrid(
                            grid = timetableGrid!!,
                            mergeLessons = userSettings.mergeLessons,
                            scaleBreaks = userSettings.scaleBreaks,
                            onSlotClick = { d, merged, slot -> selectedSlot = Triple(d, merged, slot) }
                        )
                    } else {
                        DailyListView(
                            grid = timetableGrid!!,
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
                    classHours = timetableGrid?.classHours ?: emptyList(),
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