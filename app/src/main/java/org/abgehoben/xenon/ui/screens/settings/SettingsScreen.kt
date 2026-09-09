package org.abgehoben.xenon.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.abgehoben.xenon.AppState
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userSettings by viewModel.userSettings.collectAsState()
    val appState by viewModel.appState.collectAsState()
    val lastScheduleLoadDurationMs by viewModel.lastScheduleLoadDurationMs.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    // Pre-resolve strings composably to avoid context.getString() lint errors
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

    val activeToken = (appState as? AppState.Authenticated)?.token

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section 1: Account
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

            // Section 2: Timetable
            item {
                SettingsGroupCard(title = stringResource(R.string.section_timetable)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_default_view),
                        subtitle = stringResource(if (userSettings.defaultViewWeekly) R.string.pref_view_grid else R.string.pref_view_list),
                        icon = Icons.Default.ViewAgenda,
                        onClick = { viewModel.setDefaultViewWeekly(!userSettings.defaultViewWeekly) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_merge_lessons),
                        description = stringResource(R.string.pref_merge_lessons_desc),
                        icon = Icons.Default.Layers,
                        checked = userSettings.mergeLessons,
                        onCheckedChange = { viewModel.setMergeLessons(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_weekend_advance),
                        description = stringResource(R.string.pref_weekend_advance_desc),
                        icon = Icons.Default.DateRange,
                        checked = userSettings.weekendAdvance,
                        onCheckedChange = { viewModel.setWeekendAdvance(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_scale_breaks),
                        description = stringResource(R.string.pref_scale_breaks_desc),
                        icon = Icons.Default.FormatLineSpacing,
                        checked = userSettings.scaleBreaks,
                        onCheckedChange = { viewModel.setScaleBreaks(it) }
                    )
                }
            }

            // Section: Calendar
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

            // Section: Appearance
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
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            // Section: Storage & Data
            item {
                SettingsGroupCard(title = stringResource(R.string.section_storage)) {
                    SettingsClickableItem(
                        title = stringResource(R.string.pref_clear_cache),
                        subtitle = stringResource(R.string.pref_clear_cache_desc),
                        icon = Icons.Default.CleaningServices,
                        onClick = {
                            viewModel.clearAppCache()
                            Toast.makeText(context, cacheClearedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsSwitchItem(
                        title = stringResource(R.string.pref_preload_weeks),
                        description = stringResource(R.string.pref_preload_weeks_desc),
                        icon = Icons.Default.CloudSync,
                        checked = userSettings.preloadWeeks,
                        onCheckedChange = { viewModel.setPreloadWeeks(it) }
                    )
                }
            }

            // Section: About & Debug
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
                        onClick = {
                            uriHandler.openUri("https://github.com/SirAbgehoben/Xenon")
                        }
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

            // Section: Sign Out
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Theme Mode Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.pref_theme)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = userSettings.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(stringResource(R.string.disclaimer_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.disclaimer_text)) },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text(stringResource(R.string.cd_close))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // iCal Feed Dialog
    if (showIcalDialog) {
        LaunchedEffect(Unit) {
            isLoadingIcal = true
            icalUrl = viewModel.fetchIcalUrl(renew = false)
            isLoadingIcal = false
        }

        AlertDialog(
            onDismissRequest = { showIcalDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.ical_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.ical_calendar_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Text(
                            text = if (isLoadingIcal) stringResource(R.string.ical_loading) else (icalUrl ?: "—"),
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            scope.launch {
                                isLoadingIcal = true
                                icalUrl = viewModel.fetchIcalUrl(renew = true)
                                isLoadingIcal = false
                                Toast.makeText(context, icalTokenRotatedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ical_rotate_token))
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !icalUrl.isNullOrEmpty(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(icalUrl!!))
                        Toast.makeText(context, urlCopiedMsg, Toast.LENGTH_SHORT).show()
                        showIcalDialog = false
                    }
                ) {
                    Text(stringResource(R.string.copy_url))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIcalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.logout_dialog_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Developer Debug Dialog
    if (showDebugDialog) {
        DebugDialog(
            jwtToken = activeToken,
            decodedJwtJson = remember(activeToken) { viewModel.decodeJwtPayload(activeToken) },
            bundleVersion = "PLACEHOLDERN",
            cacheStats = remember { viewModel.getTimetableCacheStats() },
            lastScheduleLoadDurationMs = lastScheduleLoadDurationMs,
            onPingServer = { viewModel.pingServer() },
            onDismiss = { showDebugDialog = false }
        )
    }
}