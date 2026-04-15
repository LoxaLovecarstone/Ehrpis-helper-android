package com.loxa.ehrpishelper

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

// Compose 계측 테스트에서 hiltViewModel()이 동작하려면
// @AndroidEntryPoint가 붙은 Activity가 필요하다.
// MainActivity를 그대로 쓰면 onCreate()의 FirebaseMessaging.subscribeToTopic()이
// 테스트 환경에서 크래시를 일으키므로, Firebase 없는 빈 Activity를 별도로 만든다.
// debug 소스셋에 두어 릴리즈 APK에는 포함되지 않게 한다.
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
