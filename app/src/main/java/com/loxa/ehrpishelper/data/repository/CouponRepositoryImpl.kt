package com.loxa.ehrpishelper.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loxa.ehrpishelper.domain.model.Coupon
import com.loxa.ehrpishelper.domain.repository.CouponRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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

                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

                val coupons = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val expiryEnd = doc.getString("expiry_end") ?: return@mapNotNull null
                        val isExpired = try {
                            LocalDateTime.parse(expiryEnd, formatter).isBefore(now)
                        } catch (e: Exception) {
                            false
                        }

                        @Suppress("UNCHECKED_CAST")
                        Coupon(
                            feedId = (doc.getLong("feed_id") ?: 0L).toInt(),
                            title = doc.getString("title") ?: "",
                            codes = (doc.get("coupons") as? List<String>) ?: emptyList(),
                            expiryStart = doc.getString("expiry_start") ?: "",
                            expiryEnd = expiryEnd,
                            link = doc.getString("link") ?: "",
                            createdDate = doc.getString("created_date") ?: "",
                            isExpired = isExpired,
                        )
                    }.getOrNull()
                }

                trySend(coupons)
            }

        awaitClose { listener.remove() }
    }
}
