import com.example.secureflow.Proxy.HealthResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface SmartProxyApi {

    @GET("api/health")
    suspend fun getHealth(): HealthResponse

    @GET("api/anomalies/{uid}")
    suspend fun getLatestAnomalies(@Path("uid") uid: String): AnomalyResponse


}
