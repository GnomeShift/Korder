package repository

import model.AuditAction
import model.AuditLog
import model.UserId

interface AuditLogRepository {
    suspend fun log(
        action: AuditAction,
        entityType: String,
        entityId: String,
        userId: UserId? = null,
        details: String? = null
    ): AuditLog

    suspend fun findByEntityId(entityId: String): List<AuditLog>
    suspend fun findByUserId(userId: UserId, pagination: Pagination): PaginatedResult<AuditLog>
    suspend fun findAll(pagination: Pagination): PaginatedResult<AuditLog>
}
