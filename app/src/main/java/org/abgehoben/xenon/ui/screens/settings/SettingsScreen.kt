package org.abgehoben.xenon.ui.screens.settings

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.local.model.ThemeMode
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.ui.screens.settings.components.SettingsClickableItem
import org.abgehoben.xenon.ui.screens.settings.components.SettingsGroupCard
import org.abgehoben.xenon.ui.screens.settings.components.SettingsSwitchItem
import org.abgehoben.xenon.ui.screens.settings.dialogs.*
import org.abgehoben.xenon.ui.theme.Dimens
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val activeToken by viewModel.jwtToken.collectAsState()

    SettingsScreen(
        userSettings = userSettings,
        activeToken = activeToken,
        lastScheduleLoadDurationMs = viewModel.lastScheduleLoadDurationMs,
        onSetThemeMode = viewModel::setThemeMode,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSetDefaultViewMode = viewModel::setDefaultViewMode,
        onSetMergeLessons = viewModel::setMergeLessons,
        onSetWeekendAdvance = viewModel::setWeekendAdvance,
        onSetScaleBreaks = viewModel::setScaleBreaks,
        onSetPreloadWeeks = viewModel::setPreloadWeeks,
        onClearAppCache = viewModel::clearAppCache,
        onFetchIcalUrl = viewModel::fetchIcalUrl,
        onGetCacheStats = viewModel::getTimetableCacheStats,
        onPingServer = viewModel::pingServer,
        onDecodeJwt = viewModel::decodeJwtPayload,
        onLogout = {
            viewModel.logout()
            onLogout()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    activeToken: String?,
    lastScheduleLoadDurationMs: Long?,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetDefaultViewMode: (TimetableViewMode) -> Unit,
    onSetMergeLessons: (Boolean) -> Unit,
    onSetWeekendAdvance: (Boolean) -> Unit,
    onSetScaleBreaks: (Boolean) -> Unit,
    onSetPreloadWeeks: (Boolean) -> Unit,
    onClearAppCache: () -> Unit,
    onFetchIcalUrl: suspend (Boolean) -> String?,
    onGetCacheStats: () -> CacheStats,
    onPingServer: suspend () -> Pair<Boolean, Long>,
    onDecodeJwt: (String?) -> String?,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current

    val cacheClearedMsg = stringResource(R.string.cache_cleared)
    val urlCopiedMsg = stringResource(R.string.url_copied)
    val icalTokenRotatedMsg = stringResource(R.string.ical_token_rotated)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showIcalDialog by remember { mutableStateOf(false) }

    var icalUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingIcal by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingStandard)
        ) {
            item {
                SettingsGroupCard(title = stringResource(R.string.section_account)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.account_logged_in_as),
                        subtitle = stringResource(R.string.user_role_student),
                        icon = Icons.Default.AccountCircle,
                        onClick = {}
                    )
                }
            }

            item {
                SettingsGroupCard(title = stringResource(R.string.section_timetable)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_default_view),
                        subtitle = stringResource(
                            if (userSettings.defaultViewMode == TimetableViewMode.WEEKLY) R.string.pref_view_grid
                            else R.string.pref_view_list
                        ),
                        icon = Icons.Default.ViewAgenda,
                        onClick = {
                            val nextMode = if (userSettings.defaultViewMode == TimetableViewMode.WEEKLY) {
                                TimetableViewMode.DAILY
                            } else {
                                TimetableViewMode.WEEKLY
                            }
                            onSetDefaultViewMode(nextMode)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_merge_lessons),
                        description = stringResource(R.string.pref_merge_lessons_desc),
                        icon = Icons.Default.Layers,
                        checked = userSettings.mergeLessons,
                        onCheckedChange = onSetMergeLessons
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_weekend_advance),
                        description = stringResource(R.string.pref_weekend_advance_desc),
                        icon = Icons.Default.DateRange,
                        checked = userSettings.weekendAdvance,
                        onCheckedChange = onSetWeekendAdvance
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_scale_breaks),
                        description = stringResource(R.string.pref_scale_breaks_desc),
                        icon = Icons.Default.FormatLineSpacing,
                        checked = userSettings.scaleBreaks,
                        onCheckedChange = onSetScaleBreaks
                    )
                }
            }

            item {
                SettingsGroupCard(title = stringResource(R.string.section_calendar)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_ical_feed),
                        subtitle = stringResource(R.string.pref_ical_feed_desc),
                        icon = Icons.Default.Share,
                        onClick = { showIcalDialog = true }
                    )
                }
            }

            item {
                SettingsGroupCard(title = stringResource(R.string.section_appearance)) {
                    val themeLabel = when (userSettings.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    }
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_theme),
                        subtitle = themeLabel,
                        icon = Icons.Default.DarkMode,
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_dynamic_color),
                        description = stringResource(R.string.pref_dynamic_color_desc),
                        icon = Icons.Default.Palette,
                        checked = userSettings.dynamicColor,
                        onCheckedChange = onSetDynamicColor
                    )
                }
            }

            item {
                SettingsGroupCard(title = stringResource(R.string.section_storage)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_clear_cache),
                        subtitle = stringResource(R.string.pref_clear_cache_desc),
                        icon = Icons.Default.CleaningServices,
                        onClick = {
                            onClearAppCache()
                            Toast.makeText(context, cacheClearedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_preload_weeks),
                        description = stringResource(R.string.pref_preload_weeks_desc),
                        icon = Icons.Default.CloudSync,
                        checked = userSettings.preloadWeeks,
                        onCheckedChange = onSetPreloadWeeks
                    )
                }
            }

            item {
                SettingsGroupCard(title = stringResource(R.string.section_about)) {
                    SettingsClickableItem(
                        title = "Xenon",
                        subtitle = stringResource(R.string.about_version, "1.0.0"),
                        icon = Icons.Default.Info,
                        onClick = { showDisclaimerDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsClickableItem(
                        title = stringResource(R.string.about_github),
                        subtitle = "SirAbgehoben/Xenon",
                        icon = Icons.Default.Code,
                        onClick = { uriHandler.openUri("https://github.com/SirAbgehoben/Xenon") }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsClickableItem(
                        title = stringResource(R.string.section_debug),
                        subtitle = stringResource(R.string.debug_subtitle),
                        icon = Icons.Default.BugReport,
                        onClick = { showDebugDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(Dimens.RadiusPill),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeightStandard)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.settings_logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingJumbo))
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = userSettings.themeMode,
            onSelectTheme = onSetThemeMode,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDisclaimerDialog) {
        DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
    }

    if (showIcalDialog) {
        LaunchedEffect(Unit) {
            isLoadingIcal = true
            icalUrl = onFetchIcalUrl(false)
            isLoadingIcal = false
        }

        IcalExportDialog(
            icalUrl = icalUrl,
            isLoading = isLoadingIcal,
            onRotateToken = {
                scope.launch {
                    isLoadingIcal = true
                    icalUrl = onFetchIcalUrl(true)
                    isLoadingIcal = false
                    Toast.makeText(context, icalTokenRotatedMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onCopyUrl = { url ->
                scope.launch {
                    val clipData = ClipData.newPlainText("ical_url", url)
                    clipboard.setClipEntry(clipData.toClipEntry())
                }
                Toast.makeText(context, urlCopiedMsg, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showIcalDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDebugDialog) {
        DebugDialog(
            jwtToken = activeToken,
            decodedJwtJson = remember(activeToken) { onDecodeJwt(activeToken) },
            bundleVersion = "PLACEHOLDER", //TODO
            cacheStats = remember { onGetCacheStats() },
            lastScheduleLoadDurationMs = lastScheduleLoadDurationMs,
            onPingServer = onPingServer,
            onDismiss = { showDebugDialog = false }
        )
    }
}