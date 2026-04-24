package com.loxa.ehrpishelper.presentation.coupon

enum class RewardType(val displayName: String, val firestoreValue: String) {
    OPAL("오팔", "오팔"),
    MIRACLE_SHADOW("기적", "기적의 그림자"),
    FATE_SHADOW("운명", "운명의 그림자"),
    ETC("기타", "기타"),
}
