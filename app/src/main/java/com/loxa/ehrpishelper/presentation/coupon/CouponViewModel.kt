package com.loxa.ehrpishelper.presentation.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.GetCouponsUseCase
import com.loxa.ehrpishelper.domain.usecase.GetUsedCodesUseCase
import com.loxa.ehrpishelper.domain.usecase.ToggleCouponUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CouponUiState {
    data object Loading : CouponUiState
    data class Success(
        val activeCoupons: List<Coupon>,
        val expiredCoupons: List<Coupon>,
        val usedCoupons: List<Coupon>,
        val usedCodes: Set<String>
    ) : CouponUiState
    data class Error(val message: String) : CouponUiState
}

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val getCouponsUseCase: GetCouponsUseCase,
    private val getUsedCodesUseCase: GetUsedCodesUseCase,
    private val toggleCouponUsageUseCase: ToggleCouponUsageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    // 앱 실행 중 순서 변경 방지 - 최초 로드 시 usedCodes 스냅샷으로 정렬 고정
    private var initialUsedCodes: Set<String>? = null

    init {
        viewModelScope.launch {
            runCatching {
                combine(
                    getCouponsUseCase(),
                    getUsedCodesUseCase()
                ) { coupons, usedCodes ->
                    if (initialUsedCodes == null) initialUsedCodes = usedCodes
                    val sortingCodes = initialUsedCodes!!

                    fun Coupon.allUsed() = codes.isNotEmpty() && codes.all { it in sortingCodes }

                    val (expired, active) = coupons.partition { it.isExpired }
                    val (activeUsed, activeNotUsed) = active.partition { it.allUsed() }
                    val (expiredUsed, expiredNotUsed) = expired.partition { it.allUsed() }

                    CouponUiState.Success(
                        // 유효 미사용 → 만료 미사용 → 유효 사용 → 만료 사용
                        activeCoupons = activeNotUsed,
                        expiredCoupons = expiredNotUsed,
                        usedCoupons = activeUsed + expiredUsed,
                        usedCodes = usedCodes
                    )
                }.collect { _uiState.value = it }
            }.onFailure { e ->
                _uiState.value = CouponUiState.Error(e.message ?: "오류가 발생했습니다.")
            }
        }
    }

    fun toggleUsage(code: String, isCurrentlyUsed: Boolean) {
        viewModelScope.launch {
            toggleCouponUsageUseCase(code, isCurrentlyUsed)
        }
    }
}
