package com.example.salattracker.di

import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler
import com.example.salattracker.scheduler.PrayerAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindPrayerAlarmScheduler(
        impl: DefaultPrayerAlarmScheduler
    ): PrayerAlarmScheduler
}
