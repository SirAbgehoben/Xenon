package org.abgehoben.xenon.di

import org.koin.dsl.module

val appModule = module {
    includes(
        platformModule,
        storageModule,
        networkModule,
        repositoryModule,
        viewModelModule
    )
}

val appModules = listOf(appModule)