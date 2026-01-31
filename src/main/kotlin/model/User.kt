package model

import kotlinx.serialization.Serializable
import java.util.*
import kotlin.time.Instant

data class User(
    val id: UserId,
    val email: Email,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val fullName: String get() = "$firstName $lastName"
}

@Serializable
@JvmInline
value class UserId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
        fun fromString(value: String): UserId = UserId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

enum class UserRole {
    USER,
    ADMIN;

    companion object {
        fun fromString(value: String): UserRole =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: USER
    }
}
