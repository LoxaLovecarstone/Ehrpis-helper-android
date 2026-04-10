# Android 단위 테스트: UseCase vs ViewModel

## 핵심 차이

| | UseCase | ViewModel |
|---|---|---|
| 반환 타입 | `Flow<T>` (단순 위임) | `StateFlow<UiState>` (상태 관리) |
| 코루틴 스코프 | 없음 | `viewModelScope` 있음 |
| 검증 대상 | 단일 값, 호출 여부 | 시간에 따른 상태 변화 순서 |
| 필요한 도구 | MockK + coroutines-test | MockK + coroutines-test + **Turbine** |

---

## 1. UseCase 테스트

### 무엇을 테스트하는가

UseCase는 대부분 Repository를 그대로 위임한다.

```kotlin
class GetCouponsUseCase @Inject constructor(
    private val couponRepository: CouponRepository
) {
    operator fun invoke(): Flow<List<Coupon>> = couponRepository.getCoupons()
}
```

검증할 것은 두 가지뿐이다:
1. Repository가 반환한 값이 그대로 나오는가
2. Repository를 정확히 1번 호출하는가

### 사용한 라이브러리

```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
```

### 주요 함수

#### `mockk()` — 인터페이스 가짜 객체 생성

테스트에서 실제 Firestore에 연결하지 않으려면 Repository를 mock으로 대체해야 한다.
`mockk()`는 인터페이스를 구현한 가짜 객체를 만들어준다.

```kotlin
val couponRepository: CouponRepository = mockk()
val useCase = GetCouponsUseCase(couponRepository)
```

#### `every { } returns` — mock 반환값 설정

mock 객체의 함수가 호출됐을 때 어떤 값을 반환할지 지정한다.

```kotlin
every { couponRepository.getCoupons() } returns flowOf(coupons)
```

#### `flowOf()` — 단일 값을 emit하는 Flow 생성

`flowOf(value)`는 값을 하나 emit하고 바로 완료되는 Flow다.
Repository가 반환하는 `Flow<List<Coupon>>`을 흉내낼 때 사용한다.

#### `.first()` — Flow에서 첫 번째 값만 꺼내기

UseCase는 단순 위임이라 Flow가 여러 값을 emit할 이유가 없다.
`.first()`로 첫 번째 값만 꺼내면 충분히 검증된다.
Turbine 없이 한 줄로 처리할 수 있다.

```kotlin
val result = useCase().first()
assertEquals(coupons, result)
```

#### `verify(exactly = 1) { }` — 호출 횟수 검증

Repository가 정확히 1번 호출됐는지 검증한다.
`exactly = 1`을 지정하면 2번 이상 호출됐을 때도 실패한다.

```kotlin
verify(exactly = 1) { couponRepository.getCoupons() }
```

#### `runTest` — 테스트에서 코루틴 실행

`suspend` 함수나 `Flow`를 테스트할 때 `runTest` 블록 안에서 실행해야 한다.
일반 `@Test` 함수는 코루틴을 지원하지 않는다.

```kotlin
@Test
fun `쿠폰 목록을 정상적으로 반환한다`() = runTest {
    every { couponRepository.getCoupons() } returns flowOf(coupons)

    val result = useCase().first()

    assertEquals(coupons, result)
    verify(exactly = 1) { couponRepository.getCoupons() }
}
```

#### `coJustRun` / `coVerify` — suspend 함수 전용 mock

`ToggleCouponUsageUseCase`처럼 반환값 없는 suspend 함수는 `every`가 아니라 `coJustRun`을 써야 한다.
호출 검증도 `verify` 대신 `coVerify`를 사용한다.

```kotlin
// 일반 함수
every { repository.someFunc() } returns value
verify { repository.someFunc() }

// suspend 함수
coJustRun { repository.markAsUsed(any()) }
coVerify(exactly = 1) { repository.markAsUsed("CODE1") }
```

`co` 접두사가 붙은 함수들이 suspend 전용이라고 기억하면 쉽다.

---

## 2. ViewModel 테스트

### 무엇을 테스트하는가

ViewModel은 `StateFlow`로 UI 상태를 관리한다.
상태가 시간에 따라 순서대로 바뀌는 것이 핵심이다.

```kotlin
@HiltViewModel
class CouponViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(getCouponsUseCase(), getUsedCodesUseCase()) { coupons, usedCodes ->
                // ...
            }.collect { _uiState.value = it }
        }
    }
}
```

상태 변화 순서:
```
Loading → Success(빈 목록) → Success(쿠폰 로드됨) → Success(사용 코드 업데이트됨)
```

`.first()`로는 첫 번째 값 하나만 꺼낼 수 있어서 이 순서를 검증할 수 없다.

### 사용한 라이브러리

```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("app.cash.turbine:turbine:1.1.0")
```

### 주요 함수

#### `Turbine` — Flow emit 순서 검증

Turbine은 Flow를 구독하면서 emit되는 값을 순서대로 꺼내 검증할 수 있게 해주는 라이브러리다.

```kotlin
viewModel.uiState.test {        // Flow 구독 시작
    awaitItem()                 // 첫 번째 emit (Loading) 꺼내기
    // ... 상태 변경 ...
    val state = awaitItem()     // 다음 emit 대기 후 꺼내기
    cancelAndIgnoreRemainingEvents()  // 남은 emit 무시하고 종료
}
```

- **`.test { }`**: Flow를 구독하고 Turbine 블록 진입
- **`awaitItem()`**: 다음 emit을 기다렸다가 꺼냄. emit이 오지 않으면 타임아웃으로 실패
- **`cancelAndIgnoreRemainingEvents()`**: 테스트 끝에 반드시 호출. 아직 처리 안 된 emit이 있어도 오류 없이 종료

#### `MutableStateFlow` — 테스트에서 Flow 값 직접 조작

UseCase를 mock할 때 `flowOf()`를 쓰면 값을 한 번만 emit하고 끝난다.
ViewModel 테스트에서는 시간이 지남에 따라 값을 여러 번 바꿔야 하므로 `MutableStateFlow`를 사용한다.

```kotlin
// 테스트 클래스 필드
private val couponsFlow = MutableStateFlow<List<Coupon>>(emptyList())
private val usedCodesFlow = MutableStateFlow<Set<String>>(emptySet())

// setUp에서 mock 연결
every { getCouponsUseCase() } returns couponsFlow
every { getUsedCodesUseCase() } returns usedCodesFlow

// 테스트 중 원하는 시점에 값 변경
couponsFlow.value = listOf(activeCoupon, expiredCoupon)
```

#### `StandardTestDispatcher` + `advanceUntilIdle()` — 코루틴 실행 시점 제어

`StandardTestDispatcher`는 코루틴을 즉시 실행하지 않고 대기시킨다.
`advanceUntilIdle()`을 호출하는 시점에 보류 중인 코루틴이 모두 실행된다.
덕분에 Flow 값 변경 → 코루틴 실행 → 상태 업데이트의 순서를 테스트에서 명확히 제어할 수 있다.

```kotlin
couponsFlow.value = listOf(activeCoupon, expiredCoupon)
testDispatcher.scheduler.advanceUntilIdle()  // 여기서 ViewModel의 combine 실행

val state = awaitItem() as CouponUiState.Success
```

#### `Dispatchers.setMain` / `resetMain` — Main Dispatcher 교체

`viewModelScope`는 기본적으로 `Dispatchers.Main`에서 동작한다.
JVM 테스트 환경에는 Android Main Dispatcher가 없어서 그냥 실행하면 예외가 발생한다.
`setMain()`으로 테스트용 디스패처로 교체하고, 테스트가 끝나면 `resetMain()`으로 원복한다.

```kotlin
@Before
fun setUp() {
    Dispatchers.setMain(testDispatcher)
}

@After
fun tearDown() {
    Dispatchers.resetMain()
}
```

UseCase는 `viewModelScope`가 없으므로 이 설정이 필요 없다.

### ViewModel 설계 의도가 테스트에 영향을 준 사례

이 ViewModel은 앱 실행 중 목록 순서가 바뀌지 않도록 `initialUsedCodes` 스냅샷을 사용한다.

```kotlin
// 최초 로드 시점의 usedCodes를 고정
if (initialUsedCodes == null) initialUsedCodes = usedCodes
val sortingCodes = initialUsedCodes!!

// 섹션 분류는 initialUsedCodes 기준
fun Coupon.allUsed() = codes.isNotEmpty() && codes.all { it in sortingCodes }
```

처음에 "세션 중 코드를 체크하면 해당 쿠폰이 `usedCoupons` 섹션으로 이동한다"는 테스트를 작성했다가 실패했다.
원인을 분석해보니 섹션 이동은 **앱 최초 로드 시점**에만 반영되는 설계였다.
세션 중 체크는 `state.usedCodes`(현재값)만 업데이트되고, 섹션(구조)은 바뀌지 않는다.

이를 반영해 테스트를 두 개로 분리했다:

```kotlin
// 1. 초기 로드 시 이미 사용된 쿠폰 → usedCoupons 섹션
// → ViewModel 생성 전에 usedCodesFlow 값을 세팅해야 initialUsedCodes에 반영됨
couponsFlow.value = listOf(activeCoupon)
usedCodesFlow.value = setOf("CODE1", "CODE2")
val viewModel = createViewModel()  // 이 시점에 initialUsedCodes = {"CODE1", "CODE2"}

// 2. 세션 중 체크 → 섹션 유지, usedCodes만 업데이트
// → ViewModel 생성 후 usedCodesFlow 값 변경
val viewModel = createViewModel()  // initialUsedCodes = emptySet()
usedCodesFlow.value = setOf("CODE1", "CODE2")  // 섹션은 안 바뀜
```

테스트가 실패했을 때 "테스트 코드가 잘못됐다"고 바로 고치지 말고,
**실제 코드의 동작 의도를 먼저 파악**하는 것이 중요하다.

---

## 3. 의존성 정리

```kotlin
// build.gradle.kts (test)
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("app.cash.turbine:turbine:1.1.0")  // ViewModel 테스트에만 필요
```

| 라이브러리 | 역할 | UseCase | ViewModel |
|---|---|---|---|
| MockK | 인터페이스 mock 생성, 호출 검증 | O | O |
| coroutines-test | `runTest`, `StandardTestDispatcher` | O | O |
| Turbine | StateFlow emit 순서 검증 | X | O |
