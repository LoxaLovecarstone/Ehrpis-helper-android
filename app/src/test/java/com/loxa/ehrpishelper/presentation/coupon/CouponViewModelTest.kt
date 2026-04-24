package com.loxa.ehrpishelper.presentation.coupon

import app.cash.turbine.test
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.GetCouponsUseCase
import com.loxa.ehrpishelper.domain.usecase.GetUsedCodesUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUnusedUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUsedUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CouponViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCouponsUseCase: GetCouponsUseCase
    private lateinit var getUsedCodesUseCase: GetUsedCodesUseCase
    private lateinit var markCouponAsUsedUseCase: MarkCouponAsUsedUseCase
    private lateinit var markCouponAsUnusedUseCase: MarkCouponAsUnusedUseCase

    // Flow를 직접 조작하기 위해 MutableStateFlow 사용
    private val couponsFlow = MutableStateFlow<List<Coupon>>(emptyList())
    private val usedCodesFlow = MutableStateFlow<Set<String>>(emptySet())

    private val activeCoupon = Coupon(
        feedId = 1,
        title = "유효 쿠폰",
        codes = listOf("CODE1", "CODE2"),
        expiryStart = "2026-04-01",
        expiryEnd = "2026-04-30 23:59",
        link = "https://example.com",
        createdDate = "2026-04-01",
        isExpired = false
    )

    private val expiredCoupon = Coupon(
        feedId = 2,
        title = "만료 쿠폰",
        codes = listOf("OLD1"),
        expiryStart = "2026-03-01",
        expiryEnd = "2026-03-31 23:59",
        link = "https://example.com",
        createdDate = "2026-03-01",
        isExpired = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getCouponsUseCase = mockk()
        getUsedCodesUseCase = mockk()
        markCouponAsUsedUseCase = mockk()
        markCouponAsUnusedUseCase = mockk()

        every { getCouponsUseCase() } returns couponsFlow
        every { getUsedCodesUseCase() } returns usedCodesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CouponViewModel(
        getCouponsUseCase,
        getUsedCodesUseCase,
        markCouponAsUsedUseCase,
        markCouponAsUnusedUseCase
    )

    @Test
    fun `초기 상태는 Loading이다`() = runTest {
        val viewModel = createViewModel()

        // 첫 emit 전 상태 확인
        assertTrue(viewModel.uiState.value is CouponUiState.Loading)
    }

    @Test
    fun `쿠폰 로드 후 유효 쿠폰과 만료 쿠폰이 분리된다`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(activeCoupon, expiredCoupon)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(listOf(activeCoupon), state.activeCoupons)
            assertEquals(listOf(expiredCoupon), state.expiredCoupons)
            assertTrue(state.usedCoupons.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `초기 로드 시 이미 모든 코드가 사용된 쿠폰은 usedCoupons 섹션에 들어간다`() = runTest {
        // ViewModel 생성 전에 이미 사용된 상태로 세팅 → initialUsedCodes에 반영됨
        couponsFlow.value = listOf(activeCoupon)
        usedCodesFlow.value = setOf("CODE1", "CODE2")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertTrue(state.activeCoupons.isEmpty())
            assertEquals(listOf(activeCoupon), state.usedCoupons)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `세션 중 코드를 사용해도 섹션은 유지되고 usedCodes만 업데이트된다`() = runTest {
        // 설계 의도: 앱 실행 중 목록 순서 변경 방지 (initialUsedCodes 스냅샷 고정)
        couponsFlow.value = listOf(activeCoupon)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Success (유효 미사용)

            usedCodesFlow.value = setOf("CODE1", "CODE2")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(listOf(activeCoupon), state.activeCoupons) // 섹션 유지
            assertTrue(state.usedCoupons.isEmpty())
            assertEquals(setOf("CODE1", "CODE2"), state.usedCodes) // usedCodes는 업데이트

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `만료일이 없는 쿠폰(expiryEnd 빈 문자열)은 유효 쿠폰 섹션에 분류된다`() = runTest {
        // Firestore의 expiry_end 필드가 null이면 CouponRepositoryImpl에서 ""로 매핑하고
        // isExpired = false로 처리한다. ViewModel도 이를 만료되지 않은 쿠폰으로 분류해야 한다.
        val unknownExpiryCoupon = Coupon(
            feedId = 3,
            title = "만료일 미정 쿠폰",
            codes = listOf("UNKNOWN1"),
            expiryStart = "",
            expiryEnd = "",
            link = "https://example.com",
            createdDate = "2026-04-01",
            isExpired = false
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(unknownExpiryCoupon)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(listOf(unknownExpiryCoupon), state.activeCoupons)
            assertTrue(state.expiredCoupons.isEmpty())
            assertTrue(state.usedCoupons.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isCurrentlyUsed가 false면 MarkCouponAsUsedUseCase를 호출한다`() = runTest {
        coJustRun { markCouponAsUsedUseCase(any()) }
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleUsage("CODE1", isCurrentlyUsed = false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { markCouponAsUsedUseCase("CODE1") }
    }

    @Test
    fun `isCurrentlyUsed가 true면 MarkCouponAsUnusedUseCase를 호출한다`() = runTest {
        coJustRun { markCouponAsUnusedUseCase(any()) }
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleUsage("CODE1", isCurrentlyUsed = true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { markCouponAsUnusedUseCase("CODE1") }
    }
}
