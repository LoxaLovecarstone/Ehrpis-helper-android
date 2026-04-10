package com.loxa.ehrpishelper.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loxa.ehrpishelper.data.local.db.AppDatabase
import com.loxa.ehrpishelper.data.local.entity.UsedCouponCodeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsedCouponDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UsedCouponDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.usedCouponDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_후_getAllUsedCodes에_포함된다() = runTest {
        dao.insert(UsedCouponCodeEntity("CODE1"))

        val result = dao.getAllUsedCodes().first()

        assertTrue(result.contains("CODE1"))
    }

    @Test
    fun delete_후_getAllUsedCodes에서_제거된다() = runTest {
        dao.insert(UsedCouponCodeEntity("CODE1"))
        dao.delete("CODE1")

        val result = dao.getAllUsedCodes().first()

        assertFalse(result.contains("CODE1"))
    }

    @Test
    fun 동일_코드_재insert_시_중복_저장되지_않는다() = runTest {
        dao.insert(UsedCouponCodeEntity("CODE1"))
        dao.insert(UsedCouponCodeEntity("CODE1"))

        val result = dao.getAllUsedCodes().first()

        assertEquals(1, result.count { it == "CODE1" })
    }

    @Test
    fun 여러_코드_insert_후_전체_조회된다() = runTest {
        dao.insert(UsedCouponCodeEntity("CODE1"))
        dao.insert(UsedCouponCodeEntity("CODE2"))
        dao.insert(UsedCouponCodeEntity("CODE3"))

        val result = dao.getAllUsedCodes().first()

        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf("CODE1", "CODE2", "CODE3")))
    }

    @Test
    fun 존재하지_않는_코드_delete_시_오류없이_동작한다() = runTest {
        dao.delete("NOT_EXIST")

        val result = dao.getAllUsedCodes().first()

        assertTrue(result.isEmpty())
    }
}
