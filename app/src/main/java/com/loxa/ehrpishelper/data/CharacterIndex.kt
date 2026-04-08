package com.loxa.ehrpishelper.data

data class CharacterIndex(
    val id: Int,
    val name_ko: String,
    val name_en: String,
    val name_cn: String,
    val rarity: Int,
    val class_id: Int,
    val element_id: Int,
    val role_id: Int,
    val icon_url: String
)