package org.abgehoben.xenon.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = Dimens.SpacingMedium, bottom = Dimens.SpacingSmall)
        )
        Surface(
            shape = RoundedCornerShape(Dimens.RadiusCard),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Dimens.ElevationLevel2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = Dimens.SpacingExtraSmall),
                content = content
            )
        }
    }
}