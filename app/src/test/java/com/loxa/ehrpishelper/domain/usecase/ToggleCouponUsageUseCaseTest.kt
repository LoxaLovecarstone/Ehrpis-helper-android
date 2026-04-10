package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleCouponUsageUseCaseTest {

    private lateinit var usedCouponRepository: UsedCouponRepository
    private lateinit var useCase: ToggleCouponUsageUseCase

    @Before
    fun setUp() {
        usedCouponRepository = mockk()
        useCase = ToggleCouponUsageUseCase(usedCouponRepository)
    }

    @Test
    fun `isUsed가 false면 markAsUsed를 호출한다`() = runTest {
        coJustRun { usedCouponRepository.markAsUsed(any()) }

        useCase("CODE1", isUsed = false)

        coVerify(exactly = 1) { usedCouponRepository.markAsUsed("CODE1") }
        coVerify(exactly = 0) { usedCouponRepository.markAsUnused(any()) }
    }

    @Test
    fun `isUsed가 true면 markAsUnused를 호출한다`() = runTest {
        coJustRun { usedCouponRepository.markAsUnused(any()) }

        useCase("CODE1", isUsed = true)

        coVerify(exactly = 1) { usedCouponRepository.markAsUnused("CODE1") }
        coVerify(exactly = 0) { usedCouponRepository.markAsUsed(any()) }
    }
}
