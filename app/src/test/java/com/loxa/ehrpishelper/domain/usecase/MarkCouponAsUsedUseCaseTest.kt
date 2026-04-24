package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MarkCouponAsUsedUseCaseTest {

    private lateinit var usedCouponRepository: UsedCouponRepository
    private lateinit var useCase: MarkCouponAsUsedUseCase

    @Before
    fun setUp() {
        usedCouponRepository = mockk()
        useCase = MarkCouponAsUsedUseCase(usedCouponRepository)
    }

    @Test
    fun `markAsUsed를 호출한다`() = runTest {
        coJustRun { usedCouponRepository.markAsUsed(any()) }

        useCase("CODE1")

        coVerify(exactly = 1) { usedCouponRepository.markAsUsed("CODE1") }
    }
}
