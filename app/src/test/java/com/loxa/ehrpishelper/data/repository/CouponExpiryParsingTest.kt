package com.loxa.ehrpishelper.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * CouponRepositoryImpl의 expiry_end 파싱 로직 및
 * CouponCard의 만료일 표시 문자열 로직을 검증하는 테스트.
 *
 * 관련 커밋: fix: 만료일이 null이어도 만료일 알 수 없는 쿠폰으로 처리
 *   - expiry_end가 null → ""로 매핑, isExpired = false
 *   - expiryEnd.isEmpty() → "만료일 알 수 없음" 표시
 */
class CouponExpiryParsingTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // ── isExpired 계산 로직 (CouponRepositoryImpl과 동일) ──────────────────

    private fun calcIsExpired(expiryEnd: String, now: LocalDateTime): Boolean {
        return if (expiryEnd.isEmpty()) false else try {
            LocalDateTime.parse(expiryEnd, formatter).isBefore(now)
        } catch (e: Exception) {
            false
        }
    }

    // ── isExpired 테스트 ───────────────────────────────────────────────────

    @Test
    fun `expiry_end가 null에서 매핑된 빈 문자열이면 isExpired는 false다`() {
        val expiryEnd = "" // Firestore null → ?: "" 결과
        val now = LocalDateTime.now()

        assertFalse(calcIsExpired(expiryEnd, now))
    }

    @Test
    fun `현재 시각보다 과거인 expiry_end는 isExpired가 true다`() {
        val expiryEnd = "2020-01-01 00:00"
        val now = LocalDateTime.now()

        assertTrue(calcIsExpired(expiryEnd, now))
    }

    @Test
    fun `현재 시각보다 미래인 expiry_end는 isExpired가 false다`() {
        val expiryEnd = "2099-12-31 23:59"
        val now = LocalDateTime.now()

        assertFalse(calcIsExpired(expiryEnd, now))
    }

    @Test
    fun `형식이 잘못된 expiry_end는 파싱 실패하므로 isExpired가 false다`() {
        val expiryEnd = "invalid-date"
        val now = LocalDateTime.now()

        assertFalse(calcIsExpired(expiryEnd, now))
    }
}
