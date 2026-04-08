package com.loxa.ehrpishelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.loxa.ehrpishelper.data.RetrofitClient
import com.loxa.ehrpishelper.ui.theme.EhrpisHelperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "알림 권한 허용됨")
        } else {
            Log.e("FCM", "알림 권한 거부됨")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 캐릭터 데이터 테스트
        lifecycleScope.launch {
            try {
                val characters = RetrofitClient.api.getCharacterIndex()
                Log.d("DATA", "캐릭터 수: ${characters.size}")
                characters.forEach {
                    Log.d("DATA", "${it.name_ko} / 직업:${it.class_id} / 속성:${it.element_id}")
                }
            } catch (e: Exception) {
                Log.e("DATA", "에러: ${e.message}")
            }
        }

        // 알림 권한 요청 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // FCM 토픽 구독
        FirebaseMessaging.getInstance().subscribeToTopic("coupons")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "쿠폰 토픽 구독 성공")
                } else {
                    Log.e("FCM", "쿠폰 토픽 구독 실패")
                }
            }

        setContent {
            EhrpisHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CouponScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CouponScreen(modifier: Modifier = Modifier) {
    Text(
        text = "에르피스 헬퍼",
        modifier = modifier
    )
}