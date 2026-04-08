package com.loxa.ehrpishelper.presentation.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.GetCouponsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CouponUiState {
    data object Loading : CouponUiState
    data class Success(val coupons: List<Coupon>) : CouponUiState
    data class Error(val message: String) : CouponUiState
}

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val getCouponsUseCase: GetCouponsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    init {
        observeCoupons()
    }

    private fun observeCoupons() {
        viewModelScope.launch {
            runCatching {
                getCouponsUseCase().collect { coupons ->
                    _uiState.value = CouponUiState.Success(coupons)
                }
            }.onFailure { e ->
                _uiState.value = CouponUiState.Error(e.message ?: "오류가 발생했습니다.")
            }
        }
    }
}
