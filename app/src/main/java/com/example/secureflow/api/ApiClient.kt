package com.example.secureflow.api

import android.content.Context
import com.example.secureflow.net.NonVpnSocketFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun build(context: Context, baseUrl: String): SecureFlowApi {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        val client = OkHttpClient.Builder()
            .socketFactory(NonVpnSocketFactory(connectivityManager))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create()) // ✅ needed for @Body data class
            .client(client)
            .build()
            .create(SecureFlowApi::class.java)
    }
}
