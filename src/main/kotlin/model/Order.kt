package model

import kotlin.time.Clock
import kotlin.time.Instant

data class Order(
    val id: OrderId,
    val userId: UserId,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val totalAmount: Money get() = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal }
    val totalItems: Int get() = items.sumOf { it.quantity }

    fun canCancel(): Boolean = status in listOf(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED
    )

    fun canConfirm(): Boolean = status == OrderStatus.PENDING

    fun cancel(): Order {
        require(canCancel()) { "Cannot cancel order in status: $status" }
        return copy(
            status = OrderStatus.CANCELLED,
            updatedAt = Clock.System.now()
        )
    }

    fun confirm(): Order {
        require(canConfirm()) { "Cannot confirm order in status: $status" }
        return copy(
            status = OrderStatus.CONFIRMED,
            updatedAt = Clock.System.now()
        )
    }

    fun ship(): Order {
        require(status == OrderStatus.CONFIRMED) { "Cannot ship order in status: $status" }
        return copy(
            status = OrderStatus.SHIPPED,
            updatedAt = Clock.System.now()
        )
    }

    fun complete(): Order {
        require(status == OrderStatus.SHIPPED) { "Cannot complete order in status: $status" }
        return copy(
            status = OrderStatus.COMPLETED,
            updatedAt = Clock.System.now()
        )
    }
}

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED
}

data class OrderItem(
    val id: OrderItemId,
    val productId: ProductId,
    val productName: String,
    val quantity: Int,
    val pricePerUnit: Money
) {
    val subtotal: Money get() = pricePerUnit * quantity

    init {
        require(quantity > 0) { "Quantity must be positive" }
    }
}
