package dto

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: List<String>? = null,
    val timestamp: String = Clock.System.now().toString()
)

@Serializable
data class ValidationErrorResponse(
    val error: String = "Validation error",
    val message: String = "Request validation failed",
    val errors: List<FieldError>,
    val timestamp: String = Clock.System.now().toString()
)

@Serializable
data class FieldError(
    val field: String,
    val message: String
)
