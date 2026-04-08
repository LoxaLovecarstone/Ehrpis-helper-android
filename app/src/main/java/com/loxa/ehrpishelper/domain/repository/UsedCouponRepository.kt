package com.loxa.ehrpishelper.domain.repository

import kotlinx.coroutines.flow.Flow

interface UsedCouponRepository {
    fun getUsedCodes(): Flow<Set<String>>
    suspend fun markAsUsed(code: String)
    suspend fun markAsUnused(code: String)
}
