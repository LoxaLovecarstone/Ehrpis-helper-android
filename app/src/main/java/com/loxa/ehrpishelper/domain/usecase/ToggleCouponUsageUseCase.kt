package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import javax.inject.Inject

class ToggleCouponUsageUseCase @Inject constructor(
    private val usedCouponRepository: UsedCouponRepository
) {
    suspend operator fun invoke(code: String, isUsed: Boolean) {
        if (isUsed) usedCouponRepository.markAsUnused(code)
        else usedCouponRepository.markAsUsed(code)
    }
}
