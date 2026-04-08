package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUsedCodesUseCase @Inject constructor(
    private val usedCouponRepository: UsedCouponRepository
) {
    operator fun invoke(): Flow<Set<String>> = usedCouponRepository.getUsedCodes()
}
