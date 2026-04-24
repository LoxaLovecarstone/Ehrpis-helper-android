package com.loxa.ehrpishelper.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private val SEOUL_ZONE = ZoneId.of("Asia/Seoul")
private val EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private const val NEW_THRESHOLD_DAYS = 1L

class CouponRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CouponRepository {

    override fun getCoupons(): Flow<List<Coupon>> = callbackFlow {
        val listener = firestore.collection("coupons")
            .orderBy("created_date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val now = ZonedDateTime.now(SEOUL_ZONE)
                val today = LocalDate.now(SEOUL_ZONE)

                val coupons = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val expiryEnd = doc.getString("expiry_end") ?: ""
                        val expiryStart = doc.getString("expiry_start") ?: ""

                        val isNew = try {
                            val started = LocalDate.parse(expiryStart, DATE_FORMATTER)
                            ChronoUnit.DAYS.between(started, today) <= NEW_THRESHOLD_DAYS
                        } catch (e: Exception) {
                            false
                        }

                        @Suppress("UNCHECKED_CAST")
                        Coupon(
                            feedId = (doc.getLong("feed_id") ?: 0L).toInt(),
                            title = doc.getString("title") ?: "",
                            codes = (doc.get("coupons") as? List<String>) ?: emptyList(),
                            expiryStart = expiryStart,
                            expiryEnd = expiryEnd,
                            link = doc.getString("link") ?: "",
                            createdDate = doc.getString("created_date") ?: "",
                            isNew = isNew,
                        )
                    }.getOrNull()
                }

                trySend(coupons)
            }

        awaitClose { listener.remove() }
    }
}
