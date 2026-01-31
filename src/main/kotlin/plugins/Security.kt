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
import service.AuthService

private val logger = KotlinLogging.logger {}

data class UserPrincipal(
    val userId: UserId,
    val email: String,
    val role: UserRole
) : Principal {
    fun isAdmin(): Boolean = role == UserRole.ADMIN
}

fun Application.configureSecurity(jwtConfig: JwtConfig, authService: AuthService) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm

            verifier(jwtConfig.verifier)

            validate { credential ->
                try {
                    val userId = credential.payload.subject
                    val email = credential.payload.getClaim("email").asString()
                    val role = credential.payload.getClaim("role").asString()

                    if (userId != null && email != null && role != null) {
                        val user = authService.findUserById(UserId.fromString(userId))

                        if (user != null && user.isActive) {
                            UserPrincipal(
                                userId = UserId.fromString(userId),
                                email = email,
                                role = UserRole.fromString(role)
                            )
                        } else {
                            logger.warn { "User not found or inactive: $userId" }
                            null
                        }
                    } else {
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

fun ApplicationCall.requireAdmin(): UserPrincipal {
    val user = requireUser()
    if (!user.isAdmin()) {
        throw AuthorizationException("You don't have permission")
    }
    return user
}

fun Route.authenticated(build: Route.() -> Unit): Route {
    return authenticate("auth-jwt") {
        build()
    }
}

fun Route.adminOnly(build: Route.() -> Unit): Route {
    return authenticate("auth-jwt") {
        build()
    }
}
