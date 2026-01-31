package dto

import kotlinx.serialization.Serializable
import model.Email
import model.User
import service.LoginCommand
import service.RegisterCommand

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
) {
    fun toCommand() = RegisterCommand(
        email = Email(email),
        password = password,
        firstName = firstName,
        lastName = lastName
    )
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
) {
    fun toCommand() = LoginCommand(
        email = Email(email),
        password = password
    )
}

@Serializable
data class AuthResponse(
    val user: UserResponse,
    val token: String,
    val tokenType: String = "Bearer"
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val role: String,
    val createdAt: String
)

fun User.toResponse() = UserResponse(
    id = id.toString(),
    email = email.value,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    role = role.name,
    createdAt = createdAt.toString()
)
