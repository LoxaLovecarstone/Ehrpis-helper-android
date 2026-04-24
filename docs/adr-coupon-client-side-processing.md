# ADR: 쿠폰 처리 로직을 클라이언트(프론트)에서 전담

> ADR = Architecture Decision Record  
> 작성 목적: 설계 선택의 근거를 기록하여 추후 회고 및 포트폴리오 자료로 활용

---

## Situation (상황)

에르피스 헬퍼 앱의 쿠폰 화면을 개발하면서 다음 세 가지 처리를 어디서 담당할지 결정해야 했다.

1. **만료 여부 판단** — Firestore의 `expiry_end` 문자열을 현재 시각과 비교해 만료됐는지 판단
2. **정렬** — 만료일 오름차순 등 사용자 편의를 위한 정렬
3. **필터링** — 유효/사용완료/만료 등 섹션 분류 및 필터칩 처리

선택지는 두 가지였다.

- **Option A**: Firestore에 `is_expired` 등 파생 필드를 추가하거나, Cloud Function/백엔드가 주기적으로 연산해서 저장
- **Option B**: Firestore는 원본 데이터만 저장하고, 모든 연산을 클라이언트(Android)에서 수행

---

## Task (과제)

앱 특성상 아래 제약 조건을 만족하는 방식을 선택해야 했다.

- **실시간 정확도**: 만료 여부는 현재 시각 기준이어야 함. 서버에서 미리 계산해두면 갱신 주기 사이에 불일치 발생 가능
- **운영 복잡도 최소화**: 백엔드(Cloud Function, 스케줄러)를 추가로 운영할 여력 없음. 1인 개발 프로젝트
- **데이터 규모**: 쿠폰은 많아봤자 수백 개 수준. 페이지네이션이나 서버 사이드 필터링이 필요한 규모가 아님
- **타임존**: 만료 기준은 KST(한국 표준시). 기기 로케일에 무관하게 일관되어야 함

---

## Action (행동)

Option B, 즉 **클라이언트 전담** 방식을 선택하고 다음과 같이 구현했다.

### 1. 만료 여부 — 프론트에서 KST 기준 실시간 계산

Firestore `is_expired` 파생 필드 추가를 검토했으나 기각.  
이유: Firestore는 시간 기반 자동 갱신을 지원하지 않으며, Cloud Function으로 주기 갱신을 구현하면 갱신 주기 사이에 `is_expired = false`인 만료 쿠폰이 존재하는 데이터 불일치가 발생한다.

대신 `CouponRepositoryImpl`에서 Firestore 스냅샷 수신 시점에 즉시 계산:

```kotlin
val seoulZone = ZoneId.of("Asia/Seoul")
val now = ZonedDateTime.now(seoulZone)
val isExpired = LocalDateTime.parse(expiryEnd, formatter).atZone(seoulZone).isBefore(now)
```

- `Locale.getDefault()` 대신 `ZoneId.of("Asia/Seoul")` 고정 → 해외 기기에서도 KST 기준 동일하게 동작

### 2. 페이지네이션 — 미도입

Firestore는 `.limit()` + `.startAfter()` 페이지네이션을 지원하지만 도입하지 않았다.  
이유: 쿠폰 수가 만 개 미만이며, Firestore `callbackFlow` 실시간 구독 구조와 페이지네이션은 궁합이 좋지 않다. 전체 수신 후 메모리에서 처리하는 것이 안드로이드 8.0 이상 기기에서 체감 성능 차이가 없다.

### 3. 정렬/필터/섹션 분류 — ViewModel에서 처리

`CouponViewModel`에서 Firestore Flow와 Room DB Flow를 `combine`으로 묶어 한 번에 연산:

```kotlin
combine(getCouponsUseCase(), getUsedCodesUseCase(), _selectedFilter) { coupons, usedCodes, filter ->
    val (expired, active) = coupons.partition { it.isExpired }
    val (activeUsed, activeNotUsed) = active.partition { coupon ->
        coupon.codes.isNotEmpty() && coupon.codes.all { it in usedCodes }
    }
    // 만료일 오름차순 정렬
    CouponUiState.Success(
        activeCoupons = activeNotUsed.sortedBy { it.expiryEnd.toExpiryDateTime() },
        usedCoupons   = activeUsed.sortedBy   { it.expiryEnd.toExpiryDateTime() },
        expiredCoupons = expired.sortedBy     { it.expiryEnd.toExpiryDateTime() },
        ...
    )
}
```

사용 여부(`usedCodes`)는 Room DB에 로컬 저장하여 서버와 완전히 분리.  
리세마라(계정 재시작) 시 서버 데이터와 무관하게 유저가 자유롭게 토글할 수 있도록 설계.

---

## Result (결과)

| 항목 | 결과 |
|------|------|
| 아키텍처 복잡도 | Firestore 스키마 단순 유지. 파생 필드/Cloud Function 없음 |
| 만료 정확도 | 앱 실행 시점 기준 실시간 정확. 갱신 주기 불일치 없음 |
| 타임존 안정성 | `Asia/Seoul` 고정으로 기기 로케일 영향 없음 |
| 성능 | 수백 개 수준의 데이터에서 체감 지연 없음 (안드로이드 8.0 기준) |
| 운영 부담 | 백엔드 스케줄러/Cloud Function 추가 운영 불필요 |
| 트레이드오프 | 데이터가 수만 건 이상으로 커지면 서버 사이드 필터링 전환 필요. 현재 게임 특성상 해당 규모 도달 가능성 낮음 |
