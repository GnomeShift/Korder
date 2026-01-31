package persistence.repository

import config.dbQuery
import model.Customer
import model.CustomerId
import model.Email
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import persistence.table.CustomersTable
import repository.CreateCustomerCommand
import repository.CustomerRepository
import repository.UpdateCustomerCommand
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class ExposedCustomerRepository : CustomerRepository {
    override suspend fun findById(id: CustomerId): Customer? = dbQuery {
        CustomersTable
            .selectAll()
            .where { CustomersTable.id eq id.value }
            .map { it.toCustomer() }
            .singleOrNull()
    }

    override suspend fun findByEmail(email: Email): Customer? = dbQuery {
        CustomersTable
            .selectAll()
            .where { CustomersTable.email.lowerCase() eq email.value.lowercase() }
            .map { it.toCustomer() }
            .singleOrNull()
    }

    override suspend fun create(command: CreateCustomerCommand): Customer = dbQuery {
        val now = Clock.System.now()
        val customerId = UUID.randomUUID()

        CustomersTable.insert {
            it[id] = customerId
            it[email] = command.email.value
            it[firstName] = command.firstName
            it[lastName] = command.lastName
            it[phone] = command.phone
            it[createdAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        findById(CustomerId(customerId))!!
    }

    override suspend fun update(id: CustomerId, command: UpdateCustomerCommand): Customer? = dbQuery {
        val updated = CustomersTable.update({ CustomersTable.id eq id.value }) { stmt ->
            command.firstName?.let { stmt[firstName] = it }
            command.lastName?.let { stmt[lastName] = it }
            command.phone?.let { stmt[phone] = it }
        }

        if (updated > 0) findById(id) else null
    }

    private fun ResultRow.toCustomer() = Customer(
        id = CustomerId(this[CustomersTable.id].value),
        email = Email(this[CustomersTable.email]),
        firstName = this[CustomersTable.firstName],
        lastName = this[CustomersTable.lastName],
        phone = this[CustomersTable.phone],
        createdAt = this[CustomersTable.createdAt].toInstant().toKotlinInstant()
    )
}
