package model

import kotlin.time.Instant

data class Customer(
    val id: CustomerId,
    val email: Email,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val createdAt: Instant
) {
    val fullName: String get() = "$firstName $lastName"
}
