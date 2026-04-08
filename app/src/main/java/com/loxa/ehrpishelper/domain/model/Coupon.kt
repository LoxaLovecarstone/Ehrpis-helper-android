package com.loxa.ehrpishelper.domain.model

data class Coupon(
    val feedId: Int,
    val title: String,
    val codes: List<String>,
    val expiryStart: String,
    val expiryEnd: String,
    val link: String,
    val createdDate: String,
    val isExpired: Boolean,
)
