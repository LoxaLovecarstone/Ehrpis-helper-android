package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCouponsUseCase @Inject constructor(
    private val couponRepository: CouponRepository
) {
    operator fun invoke(): Flow<List<Coupon>> = couponRepository.getCoupons()
}
