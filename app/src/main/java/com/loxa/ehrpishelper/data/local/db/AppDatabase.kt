package com.loxa.ehrpishelper.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.loxa.ehrpishelper.data.local.dao.UsedCouponDao
import com.loxa.ehrpishelper.data.local.entity.UsedCouponCodeEntity

@Database(
    entities = [UsedCouponCodeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usedCouponDao(): UsedCouponDao
}
