package com.loxa.ehrpishelper

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// @HiltAndroidTest를 사용하려면 기본 AndroidJUnitRunner 대신
// HiltTestApplication을 Application으로 사용하는 커스텀 러너가 필요하다.
// build.gradle.kts의 testInstrumentationRunner에 이 클래스를 지정해야 한다.
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
