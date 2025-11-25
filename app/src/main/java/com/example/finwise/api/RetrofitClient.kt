package com.example.finwise.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // ⚠️ CRITICAL: Replace this with your computer's IP found in the screenshot
    // Make sure it ends with a slash /
    private const val BASE_URL = "http://172.18.0.149:8000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}