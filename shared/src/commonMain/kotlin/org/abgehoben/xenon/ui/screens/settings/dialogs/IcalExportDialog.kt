package org.abgehoben.xenon.ui.screens.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun IcalExportDialog(
    icalUrl: String?,
    isLoading: Boolean,
    onRotateToken: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.ical_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.ical_calendar_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusSmall),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Text(
                        text = if (isLoading) stringResource(Res.string.ical_loading) else (icalUrl ?: "—"),
                        modifier = Modifier.padding(Dimens.SpacingSmall),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingStandard))
                TextButton(onClick = onRotateToken) {
                    Text(stringResource(Res.string.ical_rotate_token))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !icalUrl.isNullOrEmpty(),
                onClick = {
                    icalUrl?.let(onCopyUrl)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.copy_url))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        shape = RoundedCornerShape(Dimens.RadiusDialog)
    )
}