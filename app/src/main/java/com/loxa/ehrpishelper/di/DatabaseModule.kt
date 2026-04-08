package com.loxa.ehrpishelper.di

import android.content.Context
import androidx.room.Room
import com.loxa.ehrpishelper.data.local.dao.UsedCouponDao
import com.loxa.ehrpishelper.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ehrpis_db").build()

    @Provides
    @Singleton
    fun provideUsedCouponDao(db: AppDatabase): UsedCouponDao = db.usedCouponDao()
}
