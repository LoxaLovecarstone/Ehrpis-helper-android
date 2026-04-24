package com.loxa.ehrpishelper.presentation.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.GetCouponsUseCase
import com.loxa.ehrpishelper.domain.usecase.GetUsedCodesUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUnusedUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUsedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CouponViewModel @Inject constructor(
    private val getCouponsUseCase: GetCouponsUseCase,
    private val getUsedCodesUseCase: GetUsedCodesUseCase,
    private val markCouponAsUsedUseCase: MarkCouponAsUsedUseCase,
    private val markCouponAsUnusedUseCase: MarkCouponAsUnusedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CouponFilter.ALL)

    // 앱 실행 중 섹션 이동 방지 - 최초 로드 시 usedCodes 스냅샷으로 구분 고정
    private var initialUsedCodes: Set<String>? = null

    init {
        viewModelScope.launch {
            runCatching {
                combine(
                    getCouponsUseCase(),
                    getUsedCodesUseCase(),
                    _selectedFilter
                ) { coupons, usedCodes, filter ->
                    if (initialUsedCodes == null) initialUsedCodes = usedCodes
                    val sortingCodes = initialUsedCodes!!

                    fun Coupon.allUsed() = codes.isNotEmpty() && codes.all { it in sortingCodes }

                    val (expired, active) = coupons.partition { it.isExpired }
                    val (activeUsed, activeNotUsed) = active.partition { it.allUsed() }

                    CouponUiState.Success(
                        activeCoupons = activeNotUsed,
                        usedCoupons = activeUsed,
                        expiredCoupons = expired,
                        usedCodes = usedCodes,
                        selectedFilter = filter
                    )
                }.collect { _uiState.value = it }
            }.onFailure { e ->
                _uiState.value = CouponUiState.Error(e.message ?: "오류가 발생했습니다.")
            }
        }
    }

    fun setFilter(filter: CouponFilter) {
        _selectedFilter.value = filter
    }

    fun toggleUsage(code: String, isCurrentlyUsed: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyUsed) markCouponAsUnusedUseCase(code)
            else markCouponAsUsedUseCase(code)
        }
    }
}
