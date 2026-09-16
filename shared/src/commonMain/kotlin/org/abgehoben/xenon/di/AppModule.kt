package org.abgehoben.xenon.di

import org.koin.dsl.module

val appModule = module {
    includes(
        storageModule,
        networkModule,
        repositoryModule,
        viewModelModule
    )
}

val appModules = listOf(appModule)