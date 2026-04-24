package com.loxa.ehrpishelper.presentation.coupon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.usecase.ClearAllUsedCodesUseCase
import com.loxa.ehrpishelper.domain.usecase.GetCouponsUseCase
import com.loxa.ehrpishelper.domain.usecase.GetUsedCodesUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUnusedUseCase
import com.loxa.ehrpishelper.domain.usecase.MarkCouponAsUsedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val expiryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val distantFuture = LocalDateTime.of(9999, 12, 31, 23, 59)

private fun String.toExpiryDateTime(): LocalDateTime =
    if (isEmpty()) distantFuture
    else try { LocalDateTime.parse(this, expiryFormatter) } catch (e: Exception) { distantFuture }


@HiltViewModel
class CouponViewModel @Inject constructor(
    private val getCouponsUseCase: GetCouponsUseCase,
    private val getUsedCodesUseCase: GetUsedCodesUseCase,
    private val markCouponAsUsedUseCase: MarkCouponAsUsedUseCase,
    private val markCouponAsUnusedUseCase: MarkCouponAsUnusedUseCase,
    private val clearAllUsedCodesUseCase: ClearAllUsedCodesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CouponFilter.ALL)
    private val _selectedRewardTypes = MutableStateFlow<Set<RewardType>>(emptySet())

    init {
        viewModelScope.launch {
            runCatching {
                combine(
                    getCouponsUseCase(),
                    getUsedCodesUseCase(),
                    _selectedFilter,
                    _selectedRewardTypes,
                ) { coupons, usedCodes, filter, rewardTypes ->
                    fun Coupon.allUsed() = codes.isNotEmpty() && codes.all { it in usedCodes }

                    val rewardFiltered = if (rewardTypes.isEmpty()) coupons
                    else coupons.filter { coupon ->
                        rewardTypes.any { it.firestoreValue in coupon.rewardTypes }
                    }
                    val (used, active) = rewardFiltered.partition { it.allUsed() }

                    CouponUiState.Success(
                        activeCoupons = active.sortedBy { it.expiryEnd.toExpiryDateTime() },
                        usedCoupons = used.sortedBy { it.expiryEnd.toExpiryDateTime() },
                        usedCodes = usedCodes,
                        selectedFilter = filter,
                        selectedRewardTypes = rewardTypes,
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

    fun toggleRewardType(type: RewardType?) {
        _selectedRewardTypes.update { current ->
            when {
                type == null -> emptySet()
                type in current -> current - type
                else -> current + type
            }
        }
    }

    fun clearAllUsed() {
        viewModelScope.launch { clearAllUsedCodesUseCase() }
    }

    fun toggleUsage(code: String, isCurrentlyUsed: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyUsed) markCouponAsUnusedUseCase(code)
            else markCouponAsUsedUseCase(code)
        }
    }
}
