package com.omer.mesuper.core.di

import android.content.Context
import androidx.room.Room
import com.omer.mesuper.core.database.AppDatabase
import com.omer.mesuper.feature.agenda.data.AgendaDao
import com.omer.mesuper.feature.finance.data.FinanceDao
import com.omer.mesuper.feature.finance.data.PlanningDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mesuper.db")
            .addCallback(AppDatabase.SEED_CALLBACK)
            .build()

    @Provides
    fun provideFinanceDao(db: AppDatabase): FinanceDao = db.financeDao()

    @Provides
    fun providePlanningDao(db: AppDatabase): PlanningDao = db.planningDao()

    @Provides
    fun provideAgendaDao(db: AppDatabase): AgendaDao = db.agendaDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
}
