package com.loxa.ehrpishelper.data

import retrofit2.http.GET

interface ApiService {
    @GET("data/characters/index.json")
    suspend fun getCharacterIndex(): List<CharacterIndex>
}