package com.loxa.ehrpishelper.domain.usecase

import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetCouponsUseCaseTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var useCase: GetCouponsUseCase

    @Before
    fun setUp() {
        couponRepository = mockk()
        useCase = GetCouponsUseCase(couponRepository)
    }

    @Test
    fun `쿠폰 목록을 정상적으로 반환한다`() = runTest {
        val coupons = listOf(
            Coupon(
                feedId = 1,
                title = "테스트 쿠폰",
                codes = listOf("CODE1", "CODE2"),
                expiryStart = "2026-04-01",
                expiryEnd = "2026-04-30 23:59",
                link = "https://example.com",
                createdDate = "2026-04-01",
                isNew = false
            )
        )
        every { couponRepository.getCoupons() } returns flowOf(coupons)

        val result = useCase().first()

        assertEquals(coupons, result)
    }

    @Test
    fun `쿠폰이 없으면 빈 목록을 반환한다`() = runTest {
        every { couponRepository.getCoupons() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(emptyList<Coupon>(), result)
    }

    @Test
    fun `repository의 getCoupons를 정확히 1번 호출한다`() = runTest {
        every { couponRepository.getCoupons() } returns flowOf(emptyList())

        useCase().first()

        verify(exactly = 1) { couponRepository.getCoupons() }
    }
}
