package plugins

import dto.ErrorResponse
import dto.FieldError
import dto.ValidationErrorResponse
import exception.AuthorizationException
import exception.BusinessRuleViolationException
import exception.EntityNotFoundException
import exception.InsufficientStockException
import exception.InvalidOrderStateException
import exception.ValidationException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import javax.naming.AuthenticationException

private val logger = KotlinLogging.logger {}

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            val errors = cause.reasons.map { reason ->
                val parts = reason.split(":", limit = 2)

                if (parts.size == 2) {
                    FieldError(parts[0].trim(), parts[1].trim())
                }
                else {
                    FieldError("unknown", reason)
                }
            }
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationErrorResponse(errors = errors)
            )
        }

        exception<AuthenticationException> { call, cause ->
            logger.warn { "Authentication failed: ${cause.message}" }
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse("Unauthorized", cause.message ?: "Authentication failed")
            )
        }

        exception<AuthorizationException> { call, cause ->
            logger.warn { "Authorization failed: ${cause.message}" }
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse("Forbidden", cause.message ?: "Access denied")
            )
        }

        exception<EntityNotFoundException> { call, cause ->
            logger.warn { "Entity not found: ${cause.message}" }
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Not Found", cause.message ?: "Entity not found")
            )
        }

        exception<InsufficientStockException> { call, cause ->
            logger.warn { "Insufficient stock: ${cause.message}" }
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("Insufficient Stock", cause.message ?: "Not enough items in stock")
            )
        }

        exception<InvalidOrderStateException> { call, cause ->
            logger.warn { "Invalid order state: ${cause.message}" }
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("Invalid Order State", cause.message ?: "Cannot perform action")
            )
        }

        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationErrorResponse(errors = cause.errors.map { FieldError(it.field, it.message) })
            )
        }

        exception<BusinessRuleViolationException> { call, cause ->
            logger.warn { "Business rule violation: ${cause.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Bad Request", cause.message ?: "Bad request")
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            logger.warn { "Illegal argument: ${cause.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Bad Request", cause.message ?: "Bad request")
            )
        }

        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception: ${cause.message}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal Server Error", "An unexpected error occurred")
            )
        }
    }
}
