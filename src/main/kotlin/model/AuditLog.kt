package model

import java.util.UUID
import kotlin.time.Instant

data class AuditLog(
    val id: AuditLogId,
    val action: AuditAction,
    val entityType: String,
    val entityId: String,
    val userId: UserId?,
    val details: String?,
    val createdAt: Instant
)

@JvmInline
value class AuditLogId(val value: UUID) {
    companion object {
        fun generate(): AuditLogId = AuditLogId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}

enum class AuditAction {
    ORDER_CREATED,
    ORDER_CANCELLED,
    ORDER_CONFIRMED,
    ORDER_DELETED,
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    USER_REGISTERED,
    USER_LOGIN
}
