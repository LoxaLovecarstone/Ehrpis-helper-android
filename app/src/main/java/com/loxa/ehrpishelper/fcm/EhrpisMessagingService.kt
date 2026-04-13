package com.loxa.ehrpishelper.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loxa.ehrpishelper.EhrpisApplication.Companion.CHANNEL_ID
import com.loxa.ehrpishelper.MainActivity
import com.loxa.ehrpishelper.R

class EhrpisMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return

        val title = message.notification?.title ?: "에르피스 도우미"
        val body = message.notification?.body ?: run {
            val coupons = data["coupons"]
            if (!coupons.isNullOrBlank()) "새 쿠폰이 등록됐습니다: $coupons" else "새 쿠폰이 등록됐습니다."
        }

        showNotification(title, body, data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeBitmap = android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeBitmap)
            .setContentTitle(title)
            .setContentText(body)
            // 아래 설정을 추가해야 포그라운드에서도 헤드업이 뜰 확률이 높아짐
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        // 필요 시 서버에 토큰 전송 (현재는 토픽 구독 방식이라 불필요)
    }
}
