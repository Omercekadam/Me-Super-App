package com.omer.mesuper.core.di

import android.content.Context
import androidx.room.Room
import com.omer.mesuper.core.database.AppDatabase
import com.omer.mesuper.feature.finance.data.FinanceDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mesuper.db")
            .addCallback(AppDatabase.SEED_CALLBACK)
            .build()

    @Provides
    fun provideFinanceDao(db: AppDatabase): FinanceDao = db.financeDao()
}
