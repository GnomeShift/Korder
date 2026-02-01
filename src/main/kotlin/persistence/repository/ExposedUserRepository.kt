package persistence.repository

import config.dbQuery
import model.Email
import model.User
import model.UserId
import model.UserRole
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import persistence.table.UsersTable
import repository.CreateUserCommand
import repository.UserRepository
import java.time.ZoneOffset
import java.util.*
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class ExposedUserRepository(
    private val db: DatabaseContext
) : UserRepository {
    override suspend fun findById(id: UserId): User? = db.query {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id.value }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun findByEmail(email: Email): User? = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email.lowerCase() eq email.value.lowercase() }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun existsByEmail(email: Email): Boolean = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email.lowerCase() eq email.value.lowercase() }
            .count() > 0
    }

    override suspend fun create(command: CreateUserCommand): User = dbQuery {
        val now = Clock.System.now()
        val userId = UUID.randomUUID()

        UsersTable.insert {
            it[id] = userId
            it[email] = command.email.value
            it[passwordHash] = command.passwordHash
            it[firstName] = command.firstName
            it[lastName] = command.lastName
            it[role] = command.role.name
            it[isActive] = true
            it[createdAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        findById(UserId(userId))!!
    }

    override suspend fun updateRole(id: UserId, role: UserRole): User? = dbQuery {
        val updated = UsersTable.update({ UsersTable.id eq id.value }) {
            it[UsersTable.role] = role.name
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        if (updated > 0) findById(id) else null
    }

    override suspend fun deactivate(id: UserId): Boolean = dbQuery {
        UsersTable.update({ UsersTable.id eq id.value }) {
            it[isActive] = false
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        } > 0
    }

    private fun ResultRow.toUser() = User(
        id = UserId(this[UsersTable.id].value),
        email = Email(this[UsersTable.email]),
        passwordHash = this[UsersTable.passwordHash],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        role = UserRole.fromString(this[UsersTable.role]),
        isActive = this[UsersTable.isActive],
        createdAt = this[UsersTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[UsersTable.updatedAt].toInstant().toKotlinInstant()
    )
}
