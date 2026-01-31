package service

import model.*
import repository.PaginatedResult
import repository.Pagination

interface OrderService {
    suspend fun createOrder(command: CreateOrderCommand): Result<Order>
    suspend fun cancelOrder(orderId: OrderId): Result<Order>
    suspend fun deleteOrder(orderId: OrderId): Result<Boolean>
    suspend fun confirmOrder(orderId: OrderId): Result<Order>
    suspend fun getOrder(orderId: OrderId): Order?
    suspend fun getAllOrders(pagination: Pagination, status: OrderStatus? = null): PaginatedResult<Order>
    suspend fun getCustomerOrders(customerId: CustomerId, pagination: Pagination): PaginatedResult<Order>
}

data class CreateOrderCommand(
    val customerId: CustomerId,
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
