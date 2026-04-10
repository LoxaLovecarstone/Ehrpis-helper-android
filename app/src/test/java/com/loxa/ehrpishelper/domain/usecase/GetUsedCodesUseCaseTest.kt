package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetUsedCodesUseCaseTest {

    private lateinit var usedCouponRepository: UsedCouponRepository
    private lateinit var useCase: GetUsedCodesUseCase

    @Before
    fun setUp() {
        usedCouponRepository = mockk()
        useCase = GetUsedCodesUseCase(usedCouponRepository)
    }

    @Test
    fun `사용한 코드 목록을 정상적으로 반환한다`() = runTest {
        val usedCodes = setOf("CODE1", "CODE2")
        every { usedCouponRepository.getUsedCodes() } returns flowOf(usedCodes)

        val result = useCase().first()

        assertEquals(usedCodes, result)
    }

    @Test
    fun `사용한 코드가 없으면 빈 Set을 반환한다`() = runTest {
        every { usedCouponRepository.getUsedCodes() } returns flowOf(emptySet())

        val result = useCase().first()

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `repository의 getUsedCodes를 정확히 1번 호출한다`() = runTest {
        every { usedCouponRepository.getUsedCodes() } returns flowOf(emptySet())

        useCase().first()

        verify(exactly = 1) { usedCouponRepository.getUsedCodes() }
    }
}
