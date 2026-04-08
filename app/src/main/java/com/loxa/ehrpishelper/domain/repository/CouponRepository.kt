package com.loxa.ehrpishelper.domain.repository

import com.loxa.ehrpishelper.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

interface CouponRepository {
    fun getCoupons(): Flow<List<Coupon>>
}
