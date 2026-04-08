package com.loxa.ehrpishelper.di

import com.loxa.ehrpishelper.data.repository.CouponRepositoryImpl
import com.loxa.ehrpishelper.data.repository.UsedCouponRepositoryImpl
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCouponRepository(
        couponRepositoryImpl: CouponRepositoryImpl
    ): CouponRepository

    @Binds
    @Singleton
    abstract fun bindUsedCouponRepository(
        usedCouponRepositoryImpl: UsedCouponRepositoryImpl
    ): UsedCouponRepository
}
