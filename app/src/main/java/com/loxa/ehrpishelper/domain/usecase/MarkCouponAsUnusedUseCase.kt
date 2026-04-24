package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import javax.inject.Inject

class MarkCouponAsUnusedUseCase @Inject constructor(
    private val usedCouponRepository: UsedCouponRepository
) {
    suspend operator fun invoke(code: String) = usedCouponRepository.markAsUnused(code)
}
