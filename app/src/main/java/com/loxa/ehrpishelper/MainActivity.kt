package com.loxa.ehrpishelper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.messaging.FirebaseMessaging
import com.loxa.ehrpishelper.presentation.navigation.AppNavHost
import com.loxa.ehrpishelper.presentation.navigation.CharacterList
import com.loxa.ehrpishelper.presentation.navigation.CouponList
import com.loxa.ehrpishelper.ui.theme.EhrpisHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 알림으로 들어온 목적지를 관리하는 변수
    private var startRoute: Any = CouponList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent 확인
        handleIntent(intent)

        FirebaseMessaging.getInstance().subscribeToTopic("coupons")
            .addOnCompleteListener { /* 구독 로직 */ }
        FirebaseMessaging.getInstance().subscribeToTopic("test")
            .addOnCompleteListener { /* 테스트 알림용 */ }

        setContent {
            EhrpisHelperTheme {
                // 2. 결정된 목적지를 넘김
                AppNavHost(initialDestination = startRoute)
            }
        }
    }

    // 앱이 실행 중일 때 알림을 클릭하면 호출됨
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)

        // 실행 중일 때도 화면이 바뀌게 하려면 setContent를 다시 부르거나
        // 전역 상태(StateFlow 등)를 사용해야 하지만, 일단은 주입 방식으로 구현합니다.
        setContent {
            EhrpisHelperTheme {
                AppNavHost(initialDestination = startRoute)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val routeStr = intent?.getStringExtra("route")
        // FCM data의 "route" 값에 따라 목적지 오브젝트 매핑
        startRoute = when (routeStr) {
            "coupon_list" -> CouponList
            "character_list" -> CharacterList
            else -> CouponList
        }
    }
}
