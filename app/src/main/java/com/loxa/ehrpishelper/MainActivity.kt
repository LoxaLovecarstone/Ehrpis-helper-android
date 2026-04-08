package com.loxa.ehrpishelper

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.messaging.FirebaseMessaging
import com.loxa.ehrpishelper.presentation.navigation.AppNavHost
import com.loxa.ehrpishelper.ui.theme.EhrpisHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseMessaging.getInstance().subscribeToTopic("coupons")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d("FCM", "쿠폰 토픽 구독 성공")
                else Log.e("FCM", "쿠폰 토픽 구독 실패")
            }

        setContent {
            EhrpisHelperTheme {
                AppNavHost()
            }
        }
    }
}
