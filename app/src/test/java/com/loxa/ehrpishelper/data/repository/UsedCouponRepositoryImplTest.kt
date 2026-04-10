package com.loxa.ehrpishelper.data.repository

import com.loxa.ehrpishelper.data.local.dao.UsedCouponDao
import com.loxa.ehrpishelper.data.local.entity.UsedCouponCodeEntity
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsedCouponRepositoryImplTest {

    private lateinit var dao: UsedCouponDao
    private lateinit var repository: UsedCouponRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk()
        repository = UsedCouponRepositoryImpl(dao)
    }

    @Test
    fun `DAO의 List를 Set으로 변환해서 반환한다`() = runTest {
        every { dao.getAllUsedCodes() } returns flowOf(listOf("CODE1", "CODE2", "CODE3"))

        val result = repository.getUsedCodes().first()

        assertEquals(setOf("CODE1", "CODE2", "CODE3"), result)
    }

    @Test
    fun `중복 코드는 Set으로 변환 시 하나만 남는다`() = runTest {
        every { dao.getAllUsedCodes() } returns flowOf(listOf("CODE1", "CODE1", "CODE2"))

        val result = repository.getUsedCodes().first()

        assertEquals(setOf("CODE1", "CODE2"), result)
    }

    @Test
    fun `DAO가 빈 목록을 반환하면 빈 Set이 나온다`() = runTest {
        every { dao.getAllUsedCodes() } returns flowOf(emptyList())

        val result = repository.getUsedCodes().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `markAsUsed 호출 시 DAO insert에 올바른 entity가 전달된다`() = runTest {
        coJustRun { dao.insert(any()) }

        repository.markAsUsed("CODE1")

        coVerify { dao.insert(match { it.code == "CODE1" }) }
    }

    @Test
    fun `markAsUnused 호출 시 DAO delete에 올바른 코드가 전달된다`() = runTest {
        coJustRun { dao.delete(any()) }

        repository.markAsUnused("CODE1")

        coVerify { dao.delete("CODE1") }
    }
}
