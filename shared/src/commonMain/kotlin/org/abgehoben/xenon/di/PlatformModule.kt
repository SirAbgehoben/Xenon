package org.abgehoben.xenon.di

import org.koin.core.module.Module

/**
 * Platform-specific module contract providing:
 * - HttpClientEngine (OkHttp on Android, Darwin on iOS)
 * - DataStore<Preferences> ("sessionDataStore" and "settingsDataStore")
 * - PlatformInfo
 * - PlatformNotifier
 */
expect val platformModule: Module