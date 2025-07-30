package com.example.secureflow.Cohere

import com.example.secureflow.Cohere.CohereRequest
import com.example.secureflow.Cohere.CohereResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CohereApi {
    @Headers(
        "Authorization:tHsrImy3bKiaEpGSRlWBAyKSvuyLvk9Vqrxq1jO5",
        "Content-Type: application/json"
    )
    @POST("v1/classify")
    fun classifyDomain(@Body request: CohereRequest): Call<CohereResponse>
}