package com.loxa.ehrpishelper.presentation.coupon

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.loxa.ehrpishelper.HiltTestActivity
import com.loxa.ehrpishelper.di.DatabaseModule
import com.loxa.ehrpishelper.di.FirebaseModule
import com.loxa.ehrpishelper.di.RepositoryModule
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import com.loxa.ehrpishelper.domain.repository.UsedCouponRepository
import com.loxa.ehrpishelper.ui.theme.EhrpisHelperTheme
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
// RepositoryModule: 실제 Firestore/Room 구현 대신 @BindValue 가짜 구현으로 교체하기 위해 언인스톨
// DatabaseModule, FirebaseModule: RepositoryModule이 없으면 이 모듈들의 바인딩도 필요 없으나,
// Hilt 컴포넌트 빌드 시 미사용 바인딩 충돌을 막으려면 명시적으로 함께 언인스톨해야 한다.
@UninstallModules(RepositoryModule::class, DatabaseModule::class, FirebaseModule::class)
@RunWith(AndroidJUnit4::class)
class CouponScreenTest {

    // CouponScreen의 LaunchedEffect가 POST_NOTIFICATIONS 권한을 요청하면
    // GrantPermissionsActivity가 떠서 HiltTestActivity가 pause 상태가 된다.
    // 그러면 Compose 테스트 프레임워크가 계층을 잃고 "No compose hierarchies found" 에러가 발생한다.
    // GrantPermissionRule로 테스트 시작 전 권한을 미리 부여해서 다이얼로그가 뜨지 않게 막는다.
    @get:Rule(order = 0)
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    // hiltViewModel()은 @AndroidEntryPoint Activity 위에서만 동작한다.
    // MainActivity를 쓰면 FirebaseMessaging 초기화로 크래시가 나므로
    // debug 소스셋의 HiltTestActivity(빈 ComponentActivity)를 사용한다.
    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    private val couponsFlow = MutableStateFlow<List<Coupon>>(emptyList())

    // @UninstallModules로 언인스톨한 RepositoryModule 대신 가짜 구현을 Hilt 그래프에 주입한다.
    // MutableStateFlow를 사용해서 각 테스트에서 원하는 데이터를 직접 흘려보낼 수 있다.
    @BindValue
    @JvmField
    val couponRepository: CouponRepository = object : CouponRepository {
        override fun getCoupons(): Flow<List<Coupon>> = couponsFlow
    }

    @BindValue
    @JvmField
    val usedCouponRepository: UsedCouponRepository = object : UsedCouponRepository {
        override fun getUsedCodes(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun markAsUsed(code: String) {}
        override suspend fun markAsUnused(code: String) {}
    }

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun expiryEnd가_빈_문자열이면_만료일_알_수_없음이_표시된다() {
        couponsFlow.value = listOf(
            Coupon(
                feedId = 1,
                title = "만료일 미정 쿠폰",
                codes = listOf("CODE1"),
                expiryStart = "",
                expiryEnd = "",
                link = "https://example.com",
                createdDate = "2026-04-01",
                isExpired = false
            )
        )

        composeRule.setContent {
            EhrpisHelperTheme {
                CouponScreen()
            }
        }

        composeRule.onNodeWithText("만료일 알 수 없음").assertIsDisplayed()
    }

    @Test
    fun expiryEnd에_값이_있으면_물결표와_함께_날짜가_표시된다() {
        val expiryEnd = "2026-04-30 23:59"
        couponsFlow.value = listOf(
            Coupon(
                feedId = 2,
                title = "유효 쿠폰",
                codes = listOf("CODE2"),
                expiryStart = "2026-04-01",
                expiryEnd = expiryEnd,
                link = "https://example.com",
                createdDate = "2026-04-01",
                isExpired = false
            )
        )

        composeRule.setContent {
            EhrpisHelperTheme {
                CouponScreen()
            }
        }

        composeRule.onNodeWithText("~$expiryEnd").assertIsDisplayed()
    }
}
