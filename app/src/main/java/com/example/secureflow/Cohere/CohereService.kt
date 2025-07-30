package com.example.secureflow.Cohere

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CohereService {
    private const val BASE_URL = "https://api.cohere.ai/"

    val api: CohereApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CohereApi::class.java)
    }
}