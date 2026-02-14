package persistence.repository

import config.DatabaseContext
import model.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import persistence.table.AuditLogsTable
import repository.AuditLogRepository
import repository.PaginatedResult
import repository.Pagination
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class ExposedAuditLogRepository(
    private val db: DatabaseContext
) : AuditLogRepository {
    override suspend fun log(
        action: AuditAction,
        entityType: String,
        entityId: String,
        userId: UserId?,
        details: String?
    ): AuditLog = db.query {
        val now = Clock.System.now()
        val logId = UUID.randomUUID()

        AuditLogsTable.insert {
            it[id] = logId
            it[AuditLogsTable.action] = action.name
            it[AuditLogsTable.entityType] = entityType
            it[AuditLogsTable.entityId] = entityId
            it[AuditLogsTable.userId] = userId?.value
            it[AuditLogsTable.details] = details
            it[createdAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        AuditLog(
            id = AuditLogId(logId),
            action = action,
            entityType = entityType,
            entityId = entityId,
            userId = userId,
            details = details,
            createdAt = now
        )
    }

    override suspend fun findByEntityId(entityId: String): List<AuditLog> = db.query {
        AuditLogsTable
            .selectAll()
            .where { AuditLogsTable.entityId eq entityId }
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .map { it.toAuditLog() }
    }

    override suspend fun findByUserId(
        userId: UserId,
        pagination: Pagination
    ): PaginatedResult<AuditLog> = db.query {
        val total = AuditLogsTable
            .selectAll()
            .where { AuditLogsTable.userId eq userId.value }
            .count()

        val items = AuditLogsTable
            .selectAll()
            .where { AuditLogsTable.userId eq userId.value }
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .map { it.toAuditLog() }

        PaginatedResult(items, total, pagination.page, pagination.size)
    }

    override suspend fun findAll(pagination: Pagination): PaginatedResult<AuditLog> = db.query {
        val total = AuditLogsTable.selectAll().count()

        val items = AuditLogsTable
            .selectAll()
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .map { it.toAuditLog() }

        PaginatedResult(items, total, pagination.page, pagination.size)
    }

    private fun ResultRow.toAuditLog() = AuditLog(
        id = AuditLogId(this[AuditLogsTable.id].value),
        action = AuditAction.valueOf(this[AuditLogsTable.action]),
        entityType = this[AuditLogsTable.entityType],
        entityId = this[AuditLogsTable.entityId],
        userId = this[AuditLogsTable.userId]?.value?.let { UserId(it) },
        details = this[AuditLogsTable.details],
        createdAt = this[AuditLogsTable.createdAt].toInstant().toKotlinInstant()
    )
}
