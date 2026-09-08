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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.data.MergedSlot
import org.abgehoben.xenon.data.TimetableSlot
import org.abgehoben.xenon.ui.components.SyncErrorState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(viewModel: MainViewModel) {
    val timetableGrid by viewModel.timetableGrid.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isWeeklyView by viewModel.isWeeklyView.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedSlot by remember { mutableStateOf<Triple<Int, MergedSlot, TimetableSlot>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialPage = remember {
        val today = LocalDate.now().dayOfWeek.value
        if (today in 1..5) today - 1 else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 5 })
    val pullToRefreshState = rememberPullToRefreshState()

    val calWeek = timetableGrid?.calWeek ?: ""
    val weekType = timetableGrid?.weekType ?: ""
    val mondayDate = timetableGrid?.mondayDate ?: LocalDate.now()

    val userSettings by viewModel.userSettings.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TimetableTopBar(
                calWeek = calWeek.toString(),
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
                        LoadingIndicator()
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
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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