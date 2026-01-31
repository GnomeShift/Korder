package exception

sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class EntityNotFoundException(
    entityType: String,
    id: Any
) : DomainException("$entityType with id '$id' not found")

class InsufficientStockException(
    productId: String,
    requested: Int,
    available: Int
) : DomainException(
    "Insufficient stock for product $productId. Requested: $requested, Available: $available"
)

class InvalidOrderStateException(
    orderId: String,
    currentStatus: String,
    action: String
) : DomainException(
    "Cannot $action order $orderId in status $currentStatus"
)

class ValidationException(
    val errors: List<ValidationError>
) : DomainException("Validation failed: ${errors.joinToString { it.message }}")

data class ValidationError(
    val field: String,
    val message: String
)

class BusinessRuleViolationException(
    message: String
) : DomainException(message)
