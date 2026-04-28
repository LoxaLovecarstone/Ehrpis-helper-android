package com.loxa.ehrpishelper.presentation.coupon

import app.cash.turbine.test
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.ClearAllUsedCodesUseCase
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
    private lateinit var clearAllUsedCodesUseCase: ClearAllUsedCodesUseCase

    private val couponsFlow = MutableStateFlow<List<Coupon>>(emptyList())
    private val usedCodesFlow = MutableStateFlow<Set<String>>(emptySet())

    private val coupon1 = Coupon(
        feedId = 1,
        title = "쿠폰 1",
        codes = listOf("CODE1", "CODE2"),
        expiryStart = "2026-04-01",
        expiryEnd = "2026-04-30 23:59",
        link = "https://example.com",
        createdDate = "2026-04-01",
        isNew = false
    )

    private val coupon2 = Coupon(
        feedId = 2,
        title = "쿠폰 2",
        codes = listOf("CODE3"),
        expiryStart = "2026-04-01",
        expiryEnd = "2026-04-20 23:59",
        link = "https://example.com",
        createdDate = "2026-04-01",
        isNew = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getCouponsUseCase = mockk()
        getUsedCodesUseCase = mockk()
        markCouponAsUsedUseCase = mockk()
        markCouponAsUnusedUseCase = mockk()
        clearAllUsedCodesUseCase = mockk()

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
        markCouponAsUnusedUseCase,
        clearAllUsedCodesUseCase,
    )

    @Test
    fun `초기 상태는 Loading이다`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value is CouponUiState.Loading)
    }

    @Test
    fun `쿠폰 로드 후 미사용 쿠폰이 activeCoupons에 들어간다`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(coupon1, coupon2)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(2, state.activeCoupons.size)
            assertTrue(state.usedCoupons.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `이미 모든 코드가 사용된 쿠폰은 usedCoupons 섹션에 들어간다`() = runTest {
        couponsFlow.value = listOf(coupon1)
        usedCodesFlow.value = setOf("CODE1", "CODE2")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertTrue(state.activeCoupons.isEmpty())
            assertEquals(listOf(coupon1), state.usedCoupons)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `세션 중 모든 코드를 사용하면 activeCoupons에서 usedCoupons으로 이동한다`() = runTest {
        couponsFlow.value = listOf(coupon1)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val initial = awaitItem() as CouponUiState.Success
            assertEquals(listOf(coupon1), initial.activeCoupons)
            assertTrue(initial.usedCoupons.isEmpty())

            usedCodesFlow.value = setOf("CODE1", "CODE2")
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem() as CouponUiState.Success
            assertTrue(updated.activeCoupons.isEmpty())
            assertEquals(listOf(coupon1), updated.usedCoupons)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `만료일이 없는 쿠폰은 유효 쿠폰 섹션에 분류된다`() = runTest {
        val unknownExpiryCoupon = Coupon(
            feedId = 3,
            title = "만료일 미정 쿠폰",
            codes = listOf("UNKNOWN1"),
            expiryStart = "",
            expiryEnd = "",
            link = "https://example.com",
            createdDate = "2026-04-01",
            isNew = false
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(unknownExpiryCoupon)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(listOf(unknownExpiryCoupon), state.activeCoupons)
            assertTrue(state.usedCoupons.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `만료일이 이른 쿠폰이 먼저 정렬된다`() = runTest {
        val earlyExpiry = coupon1.copy(feedId = 10, expiryEnd = "2026-04-10 23:59")
        val lateExpiry = coupon1.copy(feedId = 11, expiryEnd = "2026-04-30 23:59")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(lateExpiry, earlyExpiry)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(earlyExpiry, state.activeCoupons[0])
            assertEquals(lateExpiry, state.activeCoupons[1])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `만료일이 없는 쿠폰은 정렬 시 맨 뒤에 온다`() = runTest {
        val withExpiry = coupon1.copy(feedId = 10, expiryEnd = "2026-04-10 23:59")
        val withoutExpiry = coupon1.copy(feedId = 11, expiryEnd = "")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading

            couponsFlow.value = listOf(withoutExpiry, withExpiry)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(withExpiry, state.activeCoupons[0])
            assertEquals(withoutExpiry, state.activeCoupons[1])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter 호출 시 selectedFilter가 UiState에 반영된다`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (기본 ALL)

            viewModel.setFilter(CouponFilter.AVAILABLE)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem() as CouponUiState.Success
            assertEquals(CouponFilter.AVAILABLE, state.selectedFilter)

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
