# Android 단위 테스트: UseCase vs ViewModel

## 핵심 차이

| | UseCase | ViewModel |
|---|---|---|
| 반환 타입 | `Flow<T>` (단순 위임) | `StateFlow<UiState>` (상태 관리) |
| 코루틴 스코프 | 없음 | `viewModelScope` 있음 |
| 검증 대상 | 단일 값, 호출 여부 | 시간에 따른 상태 변화 순서 |
| 필요한 도구 | MockK + coroutines-test | MockK + coroutines-test + **Turbine** |

---

## 1. UseCase 테스트 — `.first()`로 충분한 이유

UseCase는 대부분 Repository를 그대로 위임한다.

```kotlin
class GetCouponsUseCase @Inject constructor(
    private val couponRepository: CouponRepository
) {
    operator fun invoke(): Flow<List<Coupon>> = couponRepository.getCoupons()
}
```

테스트에서 검증할 것은 두 가지뿐이다:
1. Repository가 반환한 값이 그대로 나오는가
2. Repository를 정확히 1번 호출하는가

```kotlin
@Test
fun `쿠폰 목록을 정상적으로 반환한다`() = runTest {
    every { couponRepository.getCoupons() } returns flowOf(coupons)

    val result = useCase().first() // 첫 번째 값만 꺼내면 충분

    assertEquals(coupons, result)
    verify(exactly = 1) { couponRepository.getCoupons() }
}
```

`flowOf()`는 값을 하나 emit하고 끝나는 Flow다.
`.first()`로 첫 번째 값만 꺼내면 테스트가 완료된다.
Flow가 시간에 따라 여러 값을 emit할 필요가 없으니 Turbine이 필요 없다.

---

## 2. ViewModel 테스트 — Turbine이 필요한 이유

ViewModel은 `StateFlow`로 상태를 관리한다.

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

ViewModel은 시간에 따라 상태가 순서대로 바뀐다:

```
Loading → Success(빈 목록) → Success(쿠폰 로드됨) → Success(사용 코드 업데이트됨)
```

이 순서를 `.first()`로는 검증할 수 없다.
**Turbine**은 Flow에서 emit되는 값을 순서대로 꺼내서 검증할 수 있게 해준다.

```kotlin
@Test
fun `쿠폰 로드 후 유효 쿠폰과 만료 쿠폰이 분리된다`() = runTest {
    val viewModel = createViewModel()

    viewModel.uiState.test {        // Turbine: Flow 구독 시작
        awaitItem()                 // Loading → 소비

        couponsFlow.value = listOf(activeCoupon, expiredCoupon)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = awaitItem() as CouponUiState.Success  // 다음 emit 대기
        assertEquals(listOf(activeCoupon), state.activeCoupons)
        assertEquals(listOf(expiredCoupon), state.expiredCoupons)

        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## 3. Dispatchers.setMain이 필요한 이유

ViewModel은 내부에서 `viewModelScope.launch`를 사용한다.
`viewModelScope`는 기본적으로 `Dispatchers.Main`에서 동작한다.

테스트 환경에는 Android Main Dispatcher가 없기 때문에 그냥 실행하면 예외가 발생한다.

```kotlin
@Before
fun setUp() {
    Dispatchers.setMain(testDispatcher)  // Main → 테스트용 디스패처로 교체
}

@After
fun tearDown() {
    Dispatchers.resetMain()              // 원복
}
```

`StandardTestDispatcher`는 코루틴 실행 시점을 수동으로 제어할 수 있다.
`testDispatcher.scheduler.advanceUntilIdle()`을 호출해야 보류 중인 코루틴이 실행된다.

UseCase는 `viewModelScope`가 없으므로 이 설정이 필요 없다.

---

## 4. suspend 함수 Mock — coJustRun / coVerify

`ToggleCouponUsageUseCase`는 suspend 함수다.

```kotlin
suspend operator fun invoke(code: String, isUsed: Boolean) {
    if (isUsed) usedCouponRepository.markAsUnused(code)
    else usedCouponRepository.markAsUsed(code)
}
```

일반 함수와 suspend 함수의 MockK 문법이 다르다.

```kotlin
// 일반 함수
every { repository.someFunc() } returns value
verify { repository.someFunc() }

// suspend 함수
coJustRun { repository.markAsUsed(any()) }   // 반환값 없는 suspend 함수 mock
coVerify { repository.markAsUsed("CODE1") }  // suspend 함수 호출 검증
```

---

## 5. 의존성 정리

```kotlin
// build.gradle.kts
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("app.cash.turbine:turbine:1.1.0")  // ViewModel 테스트에만 필요
```

- **MockK**: Repository, UseCase mock 생성
- **coroutines-test**: `runTest`, `StandardTestDispatcher`, `advanceUntilIdle`
- **Turbine**: StateFlow/Flow emit 순서 검증
