data class AnomalyResponse(
    val count: Int,
    val uid: String,
    val data: List<Anomaly>
)

data class Anomaly(
    val anomaly: Boolean,
    val error: Double,
    val ip: String,
    val port: Int,
    val t: Double,
    val thr: Double,
    val uid: String
)
