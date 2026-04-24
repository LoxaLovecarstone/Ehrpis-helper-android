package com.loxa.ehrpishelper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsedCouponDao {

    @Query("SELECT code FROM used_coupon_codes")
    fun getAllUsedCodes(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.loxa.ehrpishelper.data.local.entity.UsedCouponCodeEntity)

    @Query("DELETE FROM used_coupon_codes WHERE code = :code")
    suspend fun delete(code: String)

    @Query("DELETE FROM used_coupon_codes")
    suspend fun deleteAll()
}
