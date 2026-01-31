package repository

import model.CustomerId
import model.Order
import model.OrderId
import model.OrderStatus

interface OrderRepository {
    suspend fun findById(id: OrderId): Order?
    suspend fun findByCustomerId(customerId: CustomerId, pagination: Pagination): PaginatedResult<Order>
    suspend fun findAll(pagination: Pagination, status: OrderStatus? = null): PaginatedResult<Order>
    suspend fun create(order: Order): Order
    suspend fun update(order: Order): Order
    suspend fun delete(id: OrderId): Boolean
}
