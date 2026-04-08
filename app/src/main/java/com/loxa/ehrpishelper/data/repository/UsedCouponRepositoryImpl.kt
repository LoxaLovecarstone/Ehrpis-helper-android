package com.loxa.ehrpishelper.data.repository

import com.loxa.ehrpishelper.data.local.dao.UsedCouponDao
import com.loxa.ehrpishelper.data.local.entity.UsedCouponCodeEntity
import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsedCouponRepositoryImpl @Inject constructor(
    private val dao: UsedCouponDao
) : UsedCouponRepository {

    override fun getUsedCodes(): Flow<Set<String>> =
        dao.getAllUsedCodes().map { it.toSet() }

    override suspend fun markAsUsed(code: String) {
        dao.insert(UsedCouponCodeEntity(code))
    }

    override suspend fun markAsUnused(code: String) {
        dao.delete(code)
    }
}
