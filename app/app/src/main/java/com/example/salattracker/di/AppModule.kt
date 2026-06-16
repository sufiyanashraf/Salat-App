package com.example.salattracker.di

import android.content.Context
import com.example.salattracker.data.local.PrayerTimeDao
import com.example.salattracker.data.local.SalatDatabase
import com.example.salattracker.data.remote.AladhanApiService
import com.example.salattracker.data.repository.PrayerTimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSalatDatabase(@ApplicationContext context: Context): SalatDatabase {
        return SalatDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePrayerTimeDao(database: SalatDatabase): PrayerTimeDao {
        return database.prayerTimeDao()
    }

    @Provides
    @Singleton
    fun provideAladhanApiService(): AladhanApiService {
        return AladhanApiService()
    }

    @Provides
    @Singleton
    fun providePrayerTimeRepository(
        dao: PrayerTimeDao,
        api: AladhanApiService
    ): PrayerTimeRepository {
        return PrayerTimeRepository(dao, api)
    }
}
