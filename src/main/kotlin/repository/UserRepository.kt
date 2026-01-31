package repository

import model.Email
import model.User
import model.UserId
import model.UserRole

interface UserRepository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun existsByEmail(email: Email): Boolean
    suspend fun create(command: CreateUserCommand): User
    suspend fun updateRole(id: UserId, role: UserRole): User?
    suspend fun deactivate(id: UserId): Boolean
}

data class CreateUserCommand(
    val email: Email,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole = UserRole.USER
)
