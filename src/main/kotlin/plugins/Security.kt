package plugins

import config.JwtConfig
import exception.AuthorizationException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import model.UserId
import model.UserRole
import service.UserCache

private val logger = KotlinLogging.logger {}

data class UserPrincipal(
    val userId: UserId,
    val email: String,
    val role: UserRole
) {
    fun isAdmin(): Boolean = role == UserRole.ADMIN
}

fun Application.configureSecurity(jwtConfig: JwtConfig, userCache: UserCache) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm

            verifier(jwtConfig.verifier)

            validate { credential ->
                try {
                    val userId = credential.payload.subject

                    if (userId != null) {
                        val user = userCache.getActiveUser(UserId.fromString(userId))

                        if (user != null) {
                            UserPrincipal(
                                userId = user.id,
                                email = user.email.value,
                                role = user.role
                            )
                        } else {
                            logger.warn { "User not found or inactive: $userId" }
                            null
                        }
                    } else {
                        logger.warn { "Invalid JWT claims" }
                        null
                    }
                } catch (e: Exception) {
                    logger.error(e) { "JWT validation error" }
                    null
                }
            }

            challenge { _, _ -> throw AuthorizationException("Token is invalid or expired") }
        }
    }
}

fun ApplicationCall.userPrincipal(): UserPrincipal? = principal<UserPrincipal>()

fun ApplicationCall.requireUser(): UserPrincipal =
    userPrincipal() ?: throw AuthorizationException("Authentication required")

fun Route.authenticated(build: Route.() -> Unit): Route {
    return authenticate("auth-jwt") {
        build()
    }
}

fun Route.adminOnly(build: Route.() -> Unit): Route {
    return authenticate("auth-jwt") {
        intercept(ApplicationCallPipeline.Call) {
            val principal = call.principal<UserPrincipal>()

            if (principal?.isAdmin() != true) {
                throw AuthorizationException("You don't have permission")
            }
        }
        build()
    }
}
