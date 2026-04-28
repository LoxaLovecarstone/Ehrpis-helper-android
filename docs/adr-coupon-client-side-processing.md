# ADR: 쿠폰 처리 로직을 클라이언트(프론트)에서 전담

> ADR = Architecture Decision Record  
> 작성 목적: 설계 선택의 근거를 기록하여 추후 회고 및 포트폴리오 자료로 활용

---

## Situation (상황)

에르피스 헬퍼 앱의 쿠폰 화면을 개발하면서 다음 처리를 어디서 담당할지 결정해야 했다.

1. **정렬** — 만료일 오름차순 등 사용자 편의를 위한 정렬
2. **필터링** — 사용완료/미사용 섹션 분류 및 상태/종류 필터칩 처리
3. **섹션 분류** — 유효 미사용, 사용완료 구분

선택지는 두 가지였다.

- **Option A**: Firestore에 파생 필드를 추가하거나, Cloud Function/백엔드가 주기적으로 연산해서 저장
- **Option B**: Firestore는 원본 데이터만 저장하고, 모든 연산을 클라이언트(Android)에서 수행

> **만료 여부 판단은 이 ADR의 범위에서 제외된다.**  
> 초기 설계에는 클라이언트에서 `isExpired`를 계산하는 로직이 있었으나,  
> 백엔드 GitHub Actions가 매일 만료 문서를 hard delete하는 구조로 전환하면서 제거됐다.  
> Firestore에 만료 문서가 잔존하지 않으므로 앱에서 만료 판단이 불필요해졌다.  
> 관련 내용: `docs/backend-request-cleanup-expired-coupons.md`

---

## Task (과제)

앱 특성상 아래 제약 조건을 만족하는 방식을 선택해야 했다.

- **운영 복잡도 최소화**: 백엔드(Cloud Function, 스케줄러)를 추가로 운영할 여력 없음. 1인 개발 프로젝트
- **데이터 규모**: 쿠폰은 많아봤자 수백 개 수준. 페이지네이션이나 서버 사이드 필터링이 필요한 규모가 아님
- **타임존**: 정렬 기준이 되는 만료일은 KST(한국 표준시) 기준. 기기 로케일에 무관하게 일관되어야 함

---

## Action (행동)

Option B, 즉 **클라이언트 전담** 방식을 선택하고 다음과 같이 구현했다.

### 1. 페이지네이션 — 미도입

Firestore는 `.limit()` + `.startAfter()` 페이지네이션을 지원하지만 도입하지 않았다.  
이유: 쿠폰 수가 만 개 미만이며, Firestore `callbackFlow` 실시간 구독 구조와 페이지네이션은 궁합이 좋지 않다. 전체 수신 후 메모리에서 처리하는 것이 Android 8.0 이상 기기에서 체감 성능 차이가 없다.

### 2. 정렬/필터/섹션 분류 — ViewModel에서 처리

`CouponViewModel`에서 Firestore Flow, Room DB Flow, UI 상태 Flow 4개를 `combine`으로 묶어 한 번에 연산한다.

```kotlin
combine(
    getCouponsUseCase(),       // Firestore: 쿠폰 목록
    getUsedCodesUseCase(),     // Room DB: 사용한 코드 Set
    _selectedFilter,           // UI 상태: 상태 필터 (전체/사용가능/사용완료)
    _selectedRewardTypes,      // UI 상태: 종류 필터 (오팔/기적/운명/기타, OR 연산)
) { coupons, usedCodes, filter, rewardTypes ->

    // 종류 필터 — OR 연산: 선택된 종류 중 하나라도 포함된 쿠폰
    val rewardFiltered = if (rewardTypes.isEmpty()) coupons
    else coupons.filter { coupon ->
        rewardTypes.any { it.firestoreValue in coupon.rewardTypes }
    }

    // 모든 코드가 사용됐으면 "사용완료" 섹션
    fun Coupon.allUsed() = codes.isNotEmpty() && codes.all { it in usedCodes }
    val (used, active) = rewardFiltered.partition { it.allUsed() }

    CouponUiState.Success(
        activeCoupons = active.sortedBy { it.expiryEnd.toExpiryDateTime() },
        usedCoupons   = used.sortedBy   { it.expiryEnd.toExpiryDateTime() },
        usedCodes = usedCodes,
        selectedFilter = filter,
        selectedRewardTypes = rewardTypes,
    )
}
```

사용 여부(`usedCodes`)는 Room DB에 로컬 저장하여 서버와 완전히 분리.  
리세마라(계정 재시작) 시 서버 데이터와 무관하게 유저가 자유롭게 토글할 수 있도록 설계.

### 3. 만료일 정렬 기준 — KST 명시적 처리

```kotlin
private val SEOUL_ZONE = ZoneId.of("Asia/Seoul")
private val EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun String.toExpiryDateTime(): LocalDateTime =
    if (isEmpty()) distantFuture  // 만료일 없는 쿠폰은 맨 뒤로
    else try { LocalDateTime.parse(this, EXPIRY_FORMATTER) } catch (e: Exception) { distantFuture }
```

`Locale.getDefault()` 대신 `ZoneId.of("Asia/Seoul")` 고정 → 해외 기기에서도 KST 기준 동일하게 동작.  
만료일 파싱 실패 시 `distantFuture`로 fallback해 맨 뒤에 정렬.

---

## Result (결과)

| 항목 | 결과 |
|---|---|
| 아키텍처 복잡도 | Firestore 스키마 단순 유지. 파생 필드/Cloud Function 없음 |
| 타임존 안정성 | `Asia/Seoul` 고정으로 기기 로케일 영향 없음 |
| 성능 | 수백 개 수준의 데이터에서 체감 지연 없음 (Android 8.0 기준) |
| 운영 부담 | 백엔드 스케줄러/Cloud Function 추가 운영 불필요 |
| 트레이드오프 | 데이터가 수만 건 이상으로 커지면 서버 사이드 필터링 전환 필요. 현재 게임 특성상 해당 규모 도달 가능성 낮음 |

---

## 설계 변경 이력

| 시점 | 변경 내용 | 이유 |
|---|---|---|
| 초기 | `isExpired` 클라이언트 계산, 만료 탭(`expiredCoupons`) 존재 | 만료 쿠폰을 앱에서 구분해 표시 |
| 리팩토링 | `isExpired`, `expiredCoupons`, `CouponFilter.EXPIRED` 제거 | 백엔드 GitHub Actions가 만료 문서를 hard delete → Firestore에 만료 문서가 잔존하지 않음 |
| 기능 추가 | `_selectedRewardTypes` 추가, combine 4개 Flow로 확장 | 리세마라용 보상 종류 필터(오팔/기적/운명/기타) 추가 |
