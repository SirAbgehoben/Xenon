package org.abgehoben.xenon.ui.screens.settings.dialogs

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.ui.theme.StatusSuccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DebugDialog(
    jwtToken: String?,
    decodedJwtJson: String?,
    bundleVersion: String,
    cacheStats: CacheStats,
    lastScheduleLoadDurationMs: Long?,
    onPingServer: suspend () -> Pair<Boolean, Long>,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPinging by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }

    val expInfo = remember(decodedJwtJson) {
        try {
            val expRegex = """"exp"\s*:\s*(\d+)""".toRegex()
            val match = expRegex.find(decodedJwtJson ?: "")
            match?.groupValues?.get(1)?.toLongOrNull()?.let { expSec ->
                val instant = Instant.ofEpochSecond(expSec)
                val formatted = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
                val remainingHours = (expSec - Instant.now().epochSecond) / 3600
                "$formatted (${remainingHours}h remaining)"
            }
        } catch (_: Exception) {
            null
        }
    }

    val reportCopiedMsg = "Debug-Report in die Zwischenablage kopiert"
    val tokenCopiedMsg = stringResource(R.string.debug_token_copied)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.debug_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Diagnostics & Performance Metrics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusExtraSmall),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "DEBUG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(
                            horizontal = Dimens.SpacingSmall,
                            vertical = Dimens.SpacingHairline
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingNormal)
            ) {
                // Section: Schedule Load Time
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Schedule Load Time",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingHairline))
                            Text(
                                text = if (lastScheduleLoadDurationMs != null) {
                                    "Fetched in $lastScheduleLoadDurationMs ms"
                                } else {
                                    "Not loaded in this session"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lastScheduleLoadDurationMs != null) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (lastScheduleLoadDurationMs != null) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusExtraSmall),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${lastScheduleLoadDurationMs}ms",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = Dimens.SpacingStandard,
                                        vertical = Dimens.SpacingExtraSmall
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: Server Ping
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Server Ping",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when {
                                isPinging -> "Testing connection…"
                                pingResult == null -> "Press test to measure latency"
                                pingResult!!.first -> "Online (${pingResult!!.second} ms)"
                                else -> "Unreachable (${pingResult!!.second} ms)"
                            }
                            val statusColor = when {
                                isPinging || pingResult == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                pingResult!!.first -> StatusSuccess
                                else -> MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isPinging = true
                                    pingResult = onPingServer()
                                    isPinging = false
                                }
                            },
                            enabled = !isPinging,
                            shape = RoundedCornerShape(Dimens.RadiusSmall)
                        ) {
                            Text(if (isPinging) "…" else "Test")
                        }
                    }
                }

                // Section: Memory Cache Stats
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(Dimens.SpacingNormal)) {
                        Text(
                            text = "Memory Cache Stats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingStandard))
                        CacheStatRow("Cached Weeks:", "${cacheStats.cachedWeeksCount} weeks")
                        CacheStatRow("Cached Calendar Days:", "${cacheStats.cachedCalendarDaysCount} days")
                        CacheStatRow("Loaded Courses:", "${cacheStats.coursesCount} courses")
                        CacheStatRow("School Periods (ClassHours):", "${cacheStats.classHoursCount} periods")
                        CacheStatRow("Teachers in Memory:", "${cacheStats.teachersCount} teachers")
                    }
                }

                // Section: JWT Token
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(Dimens.SpacingNormal)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.debug_jwt_token),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (jwtToken != null) {
                                        scope.launch {
                                            val clipData = ClipData.newPlainText("jwt_token", jwtToken)
                                            clipboard.setClipEntry(clipData.toClipEntry())
                                        }
                                        Toast.makeText(context, tokenCopiedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = jwtToken != null,
                                modifier = Modifier.size(Dimens.RadiusExtraLarge)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(Dimens.IconSizeSmall)
                                )
                            }
                        }

                        if (expInfo != null) {
                            Spacer(modifier = Modifier.height(Dimens.SpacingHairline))
                            Text(
                                text = "Expires: $expInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusExtraSmall),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Text(
                                text = decodedJwtJson ?: jwtToken?.take(48)?.let { "$it…" } ?: "No active session",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(Dimens.SpacingStandard),
                                maxLines = 4
                            )
                        }
                    }
                }

                // Section: Environment
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(Dimens.SpacingNormal)) {
                        Text(
                            text = "Environment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingExtraSmall))
                        Text(
                            text = "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Build Bundle Hash: $bundleVersion",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Copy Diagnostic Report
                Button(
                    onClick = {
                        val report = buildString {
                            appendLine("### Xenon Diagnostic Report")
                            appendLine("- **Timestamp:** ${Instant.now()}")
                            appendLine("- **Device:** ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
                            appendLine("- **Bundle Hash:** $bundleVersion")
                            appendLine("- **Schedule Load Duration:** ${lastScheduleLoadDurationMs?.let { "$it ms" } ?: "N/A"}")
                            appendLine("- **Has Token:** ${jwtToken != null}")
                            appendLine("- **JWT Claims:** ${decodedJwtJson ?: "none"}")
                            appendLine("- **Cached Weeks:** ${cacheStats.cachedWeeksCount}")
                            appendLine("- **Cached Calendar Days:** ${cacheStats.cachedCalendarDaysCount}")
                            appendLine("- **Courses in Memory:** ${cacheStats.coursesCount}")
                            appendLine("- **ClassHours in Memory:** ${cacheStats.classHoursCount}")
                        }
                        scope.launch {
                            val clipData = ClipData.newPlainText("debug_report", report)
                            clipboard.setClipEntry(clipData.toClipEntry())
                        }
                        Toast.makeText(context, reportCopiedMsg, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(Dimens.RadiusMedium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.ActionIconSize)
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingStandard))
                    Text("Copy Diagnostics Report")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cd_close))
            }
        },
        shape = RoundedCornerShape(Dimens.RadiusExtraLarge)
    )
}

@Composable
private fun CacheStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}