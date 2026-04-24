package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MarkCouponAsUnusedUseCaseTest {

    private lateinit var usedCouponRepository: UsedCouponRepository
    private lateinit var useCase: MarkCouponAsUnusedUseCase

    @Before
    fun setUp() {
        usedCouponRepository = mockk()
        useCase = MarkCouponAsUnusedUseCase(usedCouponRepository)
    }

    @Test
    fun `markAsUnused를 호출한다`() = runTest {
        coJustRun { usedCouponRepository.markAsUnused(any()) }

        useCase("CODE1")

        coVerify(exactly = 1) { usedCouponRepository.markAsUnused("CODE1") }
    }
}
