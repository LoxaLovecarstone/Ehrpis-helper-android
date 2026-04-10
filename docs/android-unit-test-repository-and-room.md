# Android 단위 테스트: Repository와 Room DAO

앞선 글에서 UseCase와 ViewModel 테스트를 다뤘다.
이번엔 그 아래 계층인 **Repository**와 **Room DAO** 테스트를 정리한다.

---

## 계층별 테스트 위치 정리

```
Presentation (ViewModel)   → test/       JVM
Domain (UseCase)           → test/       JVM
Data (Repository)          → test/       JVM
Data (Room DAO)            → androidTest/ 기기/에뮬레이터
```

Repository는 DAO를 mock하면 JVM에서 테스트할 수 있다.
DAO는 실제 SQLite 동작을 검증해야 하므로 기기가 필요하다.

---

## 1. Repository 테스트 — JVM

### 무엇을 테스트하는가

`UsedCouponRepositoryImpl`은 DAO를 감싸는 얇은 레이어다.

```kotlin
class UsedCouponRepositoryImpl @Inject constructor(
    private val dao: UsedCouponDao
) : UsedCouponRepository {

    override fun getUsedCodes(): Flow<Set<String>> =
        dao.getAllUsedCodes().map { it.toSet() }  // List → Set 변환

    override suspend fun markAsUsed(code: String) {
        dao.insert(UsedCouponCodeEntity(code))
    }

    override suspend fun markAsUnused(code: String) {
        dao.delete(code)
    }
}
```

검증할 것:
1. `List<String> → Set<String>` 변환이 올바른가
2. 중복 값이 Set으로 합쳐지는가
3. `markAsUsed` / `markAsUnused` 호출 시 DAO에 올바른 인자가 전달되는가

### 사용한 라이브러리

UseCase 테스트와 동일하다. 별도 추가 없음.

```kotlin
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
```

### 주요 함수

#### `match { }` — 인자 부분 검증

`markAsUsed`는 내부적으로 `UsedCouponCodeEntity(code)`를 만들어 DAO에 넘긴다.
`usedAt` 타임스탬프가 매번 달라지기 때문에 객체 전체를 비교할 수 없다.
`match { }`를 사용하면 객체의 특정 필드만 골라서 검증할 수 있다.

```kotlin
// 이렇게 하면 실패 (usedAt 값이 달라서)
coVerify { dao.insert(UsedCouponCodeEntity("CODE1")) }

// 이렇게 해야 통과 (code 필드만 검증)
coVerify { dao.insert(match { it.code == "CODE1" }) }
```

#### `flowOf()` + `.first()`

UseCase 테스트와 동일한 패턴이다.
DAO가 반환하는 `Flow<List<String>>`을 `flowOf()`로 mock하고,
`.first()`로 첫 번째 값을 꺼내서 검증한다.

```kotlin
every { dao.getAllUsedCodes() } returns flowOf(listOf("CODE1", "CODE1", "CODE2"))

val result = repository.getUsedCodes().first()

assertEquals(setOf("CODE1", "CODE2"), result)  // 중복 제거 검증
```

---

## 2. Room DAO 테스트 — androidTest

### 왜 androidTest인가

Room은 Android의 SQLite를 사용한다.
JVM에는 SQLite가 없기 때문에 mock으로 대체할 수 없다.
**실제 DB 동작**을 검증하려면 Android 환경(기기 또는 에뮬레이터)이 필요하다.

### 사용한 라이브러리

```kotlin
// androidTest에 추가
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

// 아래 두 개는 기본 포함
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.test.core)  // ApplicationProvider
```

### 인메모리 DB 세팅

```kotlin
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
```

**`Room.inMemoryDatabaseBuilder()`**
실제 파일을 생성하지 않고 메모리에만 존재하는 DB를 만든다.
테스트가 끝나면 사라지므로 테스트 간 데이터가 오염되지 않는다.

**`allowMainThreadQueries()`**
Room은 기본적으로 메인 스레드에서의 DB 접근을 금지한다.
테스트 환경에서는 이 제한을 풀어야 한다.
프로덕션 코드에는 절대 사용하면 안 된다.

**`ApplicationProvider.getApplicationContext()`**
`androidTest`에서 Context가 필요할 때 사용한다.
`@RunWith(AndroidJUnit4::class)`와 함께 써야 동작한다.

**`@RunWith(AndroidJUnit4::class)`**
androidTest에서 JUnit4 테스트를 Android 환경에서 실행하기 위한 필수 어노테이션이다.

### 테스트 케이스

```kotlin
@Test
fun insert_후_getAllUsedCodes에_포함된다() = runTest {
    dao.insert(UsedCouponCodeEntity("CODE1"))

    val result = dao.getAllUsedCodes().first()

    assertTrue(result.contains("CODE1"))
}

@Test
fun 동일_코드_재insert_시_중복_저장되지_않는다() = runTest {
    dao.insert(UsedCouponCodeEntity("CODE1"))
    dao.insert(UsedCouponCodeEntity("CODE1"))

    val result = dao.getAllUsedCodes().first()

    assertEquals(1, result.count { it == "CODE1" })
}
```

두 번째 테스트는 `@Insert(onConflict = OnConflictStrategy.REPLACE)` 설정이
실제로 동작하는지 검증한다.
mock으로는 이런 DB 레벨 동작을 검증할 수 없다.
이것이 DAO 테스트를 androidTest로 작성하는 핵심 이유다.

---

## 3. 계층별 테스트 전략 정리

| 계층 | mock 대상 | 실행 환경 | 핵심 관심사 |
|---|---|---|---|
| ViewModel | UseCase | JVM | 상태 변화 순서 |
| UseCase | Repository | JVM | 올바른 위임 |
| Repository | DAO | JVM | 타입 변환, 인자 전달 |
| DAO | 없음 (실제 DB) | androidTest | SQL 쿼리, 제약 조건 |

계층이 내려갈수록 mock 대상이 줄고, 실제 동작에 가까워진다.
DAO는 mock할 대상이 없기 때문에 실제 환경에서 돌려야 한다.
