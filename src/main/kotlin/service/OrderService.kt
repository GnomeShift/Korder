package service

import model.*
import repository.PaginatedResult
import repository.Pagination

interface OrderService {
    suspend fun createOrder(userId: UserId, command: CreateOrderCommand): Result<Order>
    suspend fun cancelOrder(userId: UserId, orderId: OrderId): Result<Order>
    suspend fun deleteOrder(userId: UserId, orderId: OrderId): Result<Boolean>
    suspend fun confirmOrder(orderId: OrderId): Result<Order>
    suspend fun getOrder(userId: UserId, orderId: OrderId): Order?
    suspend fun getAllOrders(pagination: Pagination, status: OrderStatus? = null): PaginatedResult<Order>
    suspend fun getUserOrders(userId: UserId, pagination: Pagination): PaginatedResult<Order>
}

data class CreateOrderCommand(
    val items: List<OrderItemRequest>
) {
    init {
        require(items.isNotEmpty()) { "Order must have at least one item" }
    }
}

data class OrderItemRequest(
    val productId: ProductId,
    val quantity: Int
) {
    init {
        require(quantity > 0) { "Quantity must be positive" }
    }
}
