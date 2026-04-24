package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import javax.inject.Inject

class ClearAllUsedCodesUseCase @Inject constructor(
    private val usedCouponRepository: UsedCouponRepository
) {
    suspend operator fun invoke() = usedCouponRepository.clearAll()
}
