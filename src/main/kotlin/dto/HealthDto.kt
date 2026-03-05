package dto
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val checks: HealthChecks,
    val timestamp: String
)

@Serializable
data class HealthChecks(
    val database: String
)
