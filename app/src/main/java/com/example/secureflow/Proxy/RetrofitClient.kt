package com.example.secureflow.Proxy

import SmartProxyApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://172.105.95.168:8080/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // ✅ enable Kotlin reflection for Moshi
        .build()

    val api: SmartProxyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SmartProxyApi::class.java)
    }
}
