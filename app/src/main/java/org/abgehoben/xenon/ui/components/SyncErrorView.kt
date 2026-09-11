package org.abgehoben.xenon.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.abgehoben.xenon.R
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun SyncErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(Dimens.SpacingJumbo)
    ) {
        Text(
            text = stringResource(R.string.sync_failed),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingStandard))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}