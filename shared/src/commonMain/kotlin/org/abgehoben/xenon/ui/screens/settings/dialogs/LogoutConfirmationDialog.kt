package org.abgehoben.xenon.ui.screens.settings.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.logout_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(Res.string.logout_dialog_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.settings_logout))
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