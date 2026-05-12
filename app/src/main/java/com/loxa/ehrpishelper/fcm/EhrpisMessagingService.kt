package com.loxa.ehrpishelper.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
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

        val title = data["title"] ?: "에르피스 도우미"
        val body = data["body"] ?: run {
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

        val largeBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val couponCodes = data["coupons"]
            ?.split(" ", ",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        couponCodes.take(3).forEachIndexed { index, code ->
            val copyIntent = Intent(this, CopyCouponReceiver::class.java).apply {
                putExtra(CopyCouponReceiver.EXTRA_COUPON_CODE, code)
                putExtra(CopyCouponReceiver.EXTRA_NOTIFICATION_ID, COUPON_NOTIFICATION_ID)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_COPY_BASE + index,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val label = if (couponCodes.size == 1) "복사" else "$code 복사"
            builder.addAction(0, label, copyPendingIntent)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(COUPON_NOTIFICATION_ID, builder.build())
    }

    override fun onNewToken(token: String) {
        // 필요 시 서버에 토큰 전송 (현재는 토픽 구독 방식이라 불필요)
    }

    companion object {
        private const val COUPON_NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_COPY_BASE = 100
    }
}
