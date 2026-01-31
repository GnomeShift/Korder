package dto

import kotlinx.serialization.Serializable
import model.Customer
import model.Email
import repository.CreateCustomerCommand

@Serializable
data class CustomerResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phone: String?,
    val createdAt: String
)

@Serializable
data class CreateCustomerRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null
) {
    fun toCommand() = CreateCustomerCommand(
        email = Email(email),
        firstName = firstName,
        lastName = lastName,
        phone = phone
    )
}

fun Customer.toResponse() = CustomerResponse(
    id = id.toString(),
    email = email.value,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    phone = phone,
    createdAt = createdAt.toString()
)
