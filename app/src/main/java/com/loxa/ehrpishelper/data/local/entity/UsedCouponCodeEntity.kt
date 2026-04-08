package com.loxa.ehrpishelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "used_coupon_codes")
data class UsedCouponCodeEntity(
    @PrimaryKey val code: String,
    val usedAt: Long = System.currentTimeMillis()
)
