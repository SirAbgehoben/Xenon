package org.abgehoben.xenon.ui.screens.settings.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.abgehoben.xenon.R
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.disclaimer_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.disclaimer_text)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cd_close))
            }
        },
        shape = RoundedCornerShape(Dimens.RadiusDialog)
    )
}