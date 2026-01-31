package routes

import dto.AuthResponse
import dto.ErrorResponse
import dto.LoginRequest
import dto.RegisterRequest
import dto.toResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.authenticated
import plugins.requireUser
import service.AuthService

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val result = authService.register(request.toCommand())

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    user = result.user.toResponse(),
                    token = result.token
                )
            )
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = authService.login(request.toCommand())

            call.respond(
                AuthResponse(
                    user = result.user.toResponse(),
                    token = result.token
                )
            )
        }

        authenticated {
            get("/me") {
                val principal = call.requireUser()
                val user = authService.findUserById(principal.userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Not Found", "User not found")
                    )

                call.respond(user.toResponse())
            }
        }
    }
}
