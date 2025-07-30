package com.example.secureflow.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SecureFlowApi {
    @GET("ping")
    suspend fun ping():Map<String, String>

    @POST("analyze_features")
    suspend fun analyze(@Body req: FeaturesRequest): FeaturesResponse


    @POST("forward_tcp")
    suspend fun forwardTcp(
        @Body request: TcpForwardRequest
    ): Response<TcpForwardResponse>

}
