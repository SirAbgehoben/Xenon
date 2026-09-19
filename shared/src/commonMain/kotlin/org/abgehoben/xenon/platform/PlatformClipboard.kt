package org.abgehoben.xenon.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * Converts a plain [String] into a platform-appropriate [ClipEntry]
 * for use with [androidx.compose.ui.platform.LocalClipboard].
 */
expect fun String.toClipEntry(): ClipEntry