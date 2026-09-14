package org.abgehoben.xenon.di

import org.abgehoben.xenon.data.repository.CalendarRepository
import org.abgehoben.xenon.data.repository.TimetableRepository
import org.abgehoben.xenon.data.repository.util.StudentResolver
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::StudentResolver)
    singleOf(::CalendarRepository)
    singleOf(::TimetableRepository)
}