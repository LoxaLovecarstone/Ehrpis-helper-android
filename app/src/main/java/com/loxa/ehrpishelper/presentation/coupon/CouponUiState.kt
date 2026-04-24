package com.loxa.ehrpishelper.presentation.coupon

import com.loxa.ehrpishelper.domain.model.Coupon

sealed interface CouponUiState {
    data object Loading : CouponUiState
    data class Success(
        val activeCoupons: List<Coupon>,   // 유효 + 미사용
        val usedCoupons: List<Coupon>,     // 유효 + 사용완료
        val expiredCoupons: List<Coupon>,  // 만료 (미사용 + 사용완료 합침)
        val usedCodes: Set<String>,
        val selectedFilter: CouponFilter
    ) : CouponUiState
    data class Error(val message: String) : CouponUiState
}
