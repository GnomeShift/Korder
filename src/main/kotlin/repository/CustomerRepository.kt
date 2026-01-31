package repository

import model.Customer
import model.CustomerId
import model.Email

interface CustomerRepository {
    suspend fun findById(id: CustomerId): Customer?
    suspend fun findByEmail(email: Email): Customer?
    suspend fun create(command: CreateCustomerCommand): Customer
    suspend fun update(id: CustomerId, command: UpdateCustomerCommand): Customer?
}

data class CreateCustomerCommand(
    val email: Email,
    val firstName: String,
    val lastName: String,
    val phone: String?
)

data class UpdateCustomerCommand(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null
)
