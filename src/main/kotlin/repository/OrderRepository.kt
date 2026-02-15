package repository

import model.Order
import model.OrderId
import model.OrderStatus
import model.UserId

interface OrderRepository {
    suspend fun findById(id: OrderId): Order?
    suspend fun findByUserId(userId: UserId, pagination: Pagination): PaginatedResult<Order>
    suspend fun findAll(pagination: Pagination, status: OrderStatus? = null): PaginatedResult<Order>
    suspend fun create(order: Order): Order
    suspend fun update(order: Order): Order
    suspend fun delete(id: OrderId): Boolean
}
