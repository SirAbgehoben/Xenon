package org.abgehoben.xenon.ui.screens.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.CacheStats
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
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPinging by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<Pair<Boolean, Long>?>(null) }

    // Parse expiration timestamp from JWT claims
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
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "DEBUG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION: Timetable Loading Time
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Schedule Load Time",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lastScheduleLoadDurationMs != null) {
                                    "Fetched in ${lastScheduleLoadDurationMs} ms"
                                } else {
                                    "Not loaded in this session"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lastScheduleLoadDurationMs != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (lastScheduleLoadDurationMs != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${lastScheduleLoadDurationMs}ms",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // SECTION: Live Server Ping
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                pingResult!!.first -> Color(0xFF4CAF50)
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isPinging) "…" else "Test")
                        }
                    }
                }

                // SECTION: Memory & Cache Inspector
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Memory Cache Stats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cached Weeks:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cacheStats.cachedWeeksCount} weeks", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cached Calendar Days:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cacheStats.cachedCalendarDaysCount} days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Loaded Courses:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cacheStats.coursesCount} courses", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("School Periods (ClassHours):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cacheStats.classHoursCount} periods", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Teachers in Memory:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cacheStats.teachersCount} teachers", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION: JWT Token & Expiration
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        clipboardManager.setText(AnnotatedString(jwtToken))
                                        Toast.makeText(context, tokenCopiedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = jwtToken != null,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            }
                        }

                        if (expInfo != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Expires: $expInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Text(
                                text = decodedJwtJson ?: jwtToken?.take(48)?.let { "$it…" } ?: "No active session",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 4
                            )
                        }
                    }
                }

                // SECTION: Build & Environment Info
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Environment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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

                // SECTION: Copy Diagnostics Report Button
                Button(
                    onClick = {
                        val report = buildString {
                            appendLine("### Xenon Diagnostic Report")
                            appendLine("- **Timestamp:** ${Instant.now()}")
                            appendLine("- **Device:** ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
                            appendLine("- **Bundle Hash:** $bundleVersion")
                            appendLine("- **Schedule Load Duration:** ${lastScheduleLoadDurationMs?.let { "${it} ms" } ?: "N/A"}")
                            appendLine("- **Has Token:** ${jwtToken != null}")
                            appendLine("- **JWT Claims:** ${decodedJwtJson ?: "none"}")
                            appendLine("- **Cached Weeks:** ${cacheStats.cachedWeeksCount}")
                            appendLine("- **Cached Calendar Days:** ${cacheStats.cachedCalendarDaysCount}")
                            appendLine("- **Courses in Memory:** ${cacheStats.coursesCount}")
                            appendLine("- **ClassHours in Memory:** ${cacheStats.classHoursCount}")
                        }
                        clipboardManager.setText(AnnotatedString(report))
                        Toast.makeText(context, reportCopiedMsg, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Diagnostics Report")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cd_close))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}