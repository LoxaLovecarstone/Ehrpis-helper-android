# WorkManager 기반 만료 임박 알림 — 구현 기록 및 포트폴리오

> **상태**: 구현 완료 후 롤백. 추후 재적용 예정.  
> **롤백 사유**: Firestore 문서당 과금 구조 발견 → 백엔드 만료 쿠폰 정리 선행 필요.

---

## 기능 목표
매일 낮 12시(KST)에 오늘 또는 내일 만료되는 쿠폰 중 미사용 코드가 남아 있는 항목을 로컬 알림으로 발송한다.

---

## WorkManager를 선택한 이유

### 무엇인가
Android Jetpack의 **보장형 백그라운드 작업 API**. 앱이 종료되거나 기기가 재부팅돼도 예약된 작업이 보존된다. 내부적으로 Android 버전에 따라 JobScheduler(API 23+) 또는 AlarmManager로 위임하는 래퍼 역할이다.

### 선택 근거

| 근거 | 설명 |
|---|---|
| **생존성** | 프로세스 종료, 기기 재시작 이후에도 스케줄이 유지됨. AlarmManager+BroadcastReceiver+BOOT_COMPLETED를 직접 조합하지 않아도 됨 |
| **Hilt 연동** | `@HiltWorker` + `@AssistedInject`로 UseCase 등 의존성 주입 가능 |
| **배터리 친화** | Doze/App Standby를 존중하는 방식으로 OS가 발동 시점을 조율함 |
| **클라이언트 자립** | 만료일을 앱이 이미 알고 있으므로 서버 없이 구현 가능 |

### 탈락한 대안

| 대안 | 탈락 이유 |
|---|---|
| **Handler / coroutine delay** | 앱 프로세스 종료 시 소멸. 스케줄 보존 불가 |
| **AlarmManager 직접** | 재부팅 복구를 위한 BOOT_COMPLETED 리시버 직접 구현 필요. Android 12+는 `SCHEDULE_EXACT_ALARM` 권한 별도 요청. WorkManager가 내부적으로 AlarmManager를 이미 사용하므로 중복 |
| **Foreground Service** | 알림 체크는 수 초짜리 단발 작업 → 상시 실행 서비스는 과함. 배터리·UX 불이익 |
| **FCM Cloud Function** | 서버에서 만료일을 추적해 FCM 전송 → 백엔드 비용·복잡도 추가. 만료일을 앱이 이미 보유하므로 불필요한 네트워크 의존 |

---

## 구현 구조

```
EhrpisApplication (Configuration.Provider)
  └── scheduleExpiringCouponWorker()
        └── PeriodicWorkRequest (24h, 최초 발동: 다음 낮 12시 KST)
              └── ExpiringCouponWorker (HiltWorker)
                    ├── GetCouponsUseCase → Firestore callbackFlow.first()
                    ├── GetUsedCodesUseCase → Room Flow.first()
                    └── NotificationManager → coupon_expiry_channel
```

### 핵심 포인트: Hilt + WorkManager 연동
WorkManager의 기본 초기화를 비활성화하고 Hilt의 `HiltWorkerFactory`로 대체해야 Worker 내부에서 DI가 작동한다.

```xml
<!-- AndroidManifest.xml: 기본 WorkManager 초기화 제거 -->
<provider android:name="androidx.startup.InitializationProvider" ...>
    <meta-data android:name="androidx.work.WorkManagerInitializer" tools:node="remove" />
</provider>
```

```kotlin
// EhrpisApplication: Configuration.Provider 구현
@HiltAndroidApp
class EhrpisApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
```

### 초기 지연 계산 (낮 12시 KST 고정)
```kotlin
val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
val nextNoon = now.toLocalDate().atTime(12, 0).atZone(seoulZone)
    .let { if (it.isAfter(now)) it else it.plusDays(1) }
val initialDelayMs = nextNoon.toInstant().toEpochMilli() - System.currentTimeMillis()

PeriodicWorkRequestBuilder<ExpiringCouponWorker>(1, TimeUnit.DAYS)
    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
    .build()
```

### 워커 내 데이터 조회 구조
```kotlin
// callbackFlow + first(): Firestore 리스너를 붙여 첫 응답을 받는 즉시 리스너 해제.
// 쿠폰은 로컬 저장소가 아닌 Firestore에서 직접 조회한다.
// 네트워크 없으면 Firestore SDK 오프라인 캐시에서 응답하거나, 캐시도 없으면 타임아웃.
val coupons = getCouponsUseCase().first()
// 사용한 코드는 Room DB 로컬에서 읽는다.
val usedCodes = getUsedCodesUseCase().first()
```

### 만료 임박 필터 로직
```kotlin
val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
val tomorrow = today.plusDays(1)

val expiring = coupons.filter { coupon ->
    if (coupon.isExpired) return@filter false
    if (coupon.codes.none { it !in usedCodes }) return@filter false  // 전부 사용완료면 제외
    val expiryDate = LocalDateTime.parse(coupon.expiryEnd, formatter)
        .atZone(seoulZone).toLocalDate()
    expiryDate in today..tomorrow  // Kotlin range 문법으로 기간 체크
}
```

---

## 롤백 배경: Firestore 과금 구조 문제

### 발견한 문제
Firestore는 **쿼리당** 과금이 아니라 **문서당** 과금이다. 컬렉션 전체를 구독하는 현재 구조에서:

- 쿠폰 문서 20개 × 앱 실행 1회 = **읽기 20회**
- 워커 1회 실행 = **읽기 20회**
- Spark 무료 플랜: **일 50,000회 읽기**

쿠폰이 쌓일수록 실행당 비용이 선형으로 증가한다. 만료된 쿠폰을 Firestore에 무기한 보관하면 장기적으로 무료 한도를 압박한다.

### 해결 방향 (백엔드 선행 작업)
백엔드에서 만료 쿠폰을 주기적으로 삭제하는 GitHub Actions 워크플로우를 추가한다.

```yaml
# .github/workflows/cleanup-expired-coupons.yml
name: Cleanup Expired Coupons
on:
  schedule:
    - cron: '0 16 * * *'  # 매일 01:00 KST (UTC+9 → UTC 16:00)
jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Delete expired coupons from Firestore
        # expiry_end < now(KST) 인 문서 조회 후 일괄 삭제
```

이 작업이 선행되면 Firestore 문서 수가 항상 소규모로 유지되어 과금 문제가 해소된다.

---

## 재적용 시 체크리스트

- [ ] 백엔드: 만료 쿠폰 자동 삭제 GitHub Action 완료 확인
- [ ] `gradle/libs.versions.toml`: `workManager`, `hiltWork` 버전 및 라이브러리 항목 추가
- [ ] `app/build.gradle.kts`: `work-runtime-ktx`, `hilt-work`, `hilt-androidx-compiler` 의존성 추가
- [ ] `AndroidManifest.xml`: 기본 WorkManager 초기화 비활성화 provider 추가
- [ ] `EhrpisApplication.kt`: `Configuration.Provider` 구현, expiry 채널 등록, 워커 스케줄
- [ ] `ExpiringCouponWorker.kt`: HiltWorker 생성
- [ ] 테스트: Firestore에 오늘 만료 쿠폰 문서 추가 후 알림 수신 확인 (실기기 검증 완료)

---

## 테스트 결과
실기기(앱 종료 상태)에서 WorkManager 발동 후 알림 수신 **확인 완료**.  
`PeriodicWorkRequest` 최소 주기 15분 제약으로 인해, 테스트 시에는 `OneTimeWorkRequest`를 `BuildConfig.DEBUG` 조건부 메뉴 버튼으로 즉시 발동하는 방식 사용.
