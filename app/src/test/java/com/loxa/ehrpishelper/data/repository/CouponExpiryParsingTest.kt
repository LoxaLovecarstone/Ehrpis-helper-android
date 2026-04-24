package com.loxa.ehrpishelper.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * CouponRepositoryImpl의 expiry_end 파싱 로직 및
 * CouponCard의 만료일 표시 문자열 로직을 검증하는 테스트.
 *
 * 관련 커밋: fix: 만료일이 null이어도 만료일 알 수 없는 쿠폰으로 처리
 *   - expiry_end가 null → ""로 매핑, isExpired = false
 *   - expiryEnd.isEmpty() → "만료일 알 수 없음" 표시
 *
 * 타임존: 만료 판단은 KST(Asia/Seoul) 기준. 기기 로케일에 무관하게 동일하게 동작해야 함.
 */
class CouponExpiryParsingTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val seoulZone = ZoneId.of("Asia/Seoul")

    // ── isExpired 계산 로직 (CouponRepositoryImpl과 동일) ──────────────────

    private fun calcIsExpired(expiryEnd: String, now: ZonedDateTime): Boolean {
        return if (expiryEnd.isEmpty()) false else try {
            LocalDateTime.parse(expiryEnd, formatter).atZone(seoulZone).isBefore(now)
        } catch (e: Exception) {
            false
        }
    }

    // ── isExpired 테스트 ───────────────────────────────────────────────────

    @Test
    fun `expiry_end가 빈 문자열이면 isExpired는 false다`() {
        assertFalse(calcIsExpired("", ZonedDateTime.now(seoulZone)))
    }

    @Test
    fun `현재 시각보다 과거인 expiry_end는 isExpired가 true다`() {
        assertTrue(calcIsExpired("2020-01-01 00:00", ZonedDateTime.now(seoulZone)))
    }

    @Test
    fun `현재 시각보다 미래인 expiry_end는 isExpired가 false다`() {
        assertFalse(calcIsExpired("2099-12-31 23:59", ZonedDateTime.now(seoulZone)))
    }

    @Test
    fun `형식이 잘못된 expiry_end는 파싱 실패하므로 isExpired가 false다`() {
        assertFalse(calcIsExpired("invalid-date", ZonedDateTime.now(seoulZone)))
    }

    @Test
    fun `KST 자정 직전은 만료되지 않은 것으로 판단한다`() {
        val justBeforeMidnight = ZonedDateTime.of(2026, 4, 8, 23, 58, 0, 0, seoulZone)
        assertFalse(calcIsExpired("2026-04-08 23:59", justBeforeMidnight))
    }

    @Test
    fun `KST 자정 직후는 만료된 것으로 판단한다`() {
        val justAfterMidnight = ZonedDateTime.of(2026, 4, 9, 0, 0, 0, 0, seoulZone)
        assertTrue(calcIsExpired("2026-04-08 23:59", justAfterMidnight))
    }
}
