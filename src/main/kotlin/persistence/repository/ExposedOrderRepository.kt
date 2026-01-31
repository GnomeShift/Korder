package persistence.repository

import config.dbQuery
import model.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.*
import persistence.table.OrderItemsTable
import persistence.table.OrdersTable
import repository.OrderRepository
import repository.PaginatedResult
import repository.Pagination
import java.util.UUID
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class ExposedOrderRepository : OrderRepository {
    override suspend fun findById(id: OrderId): Order? = dbQuery {
        val orderRow = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq id.value }
            .singleOrNull() ?: return@dbQuery null

        val items = OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.orderId eq id.value }
            .map { it.toOrderItem() }

        orderRow.toOrder(items)
    }

    override suspend fun findByUserId(
        userId: UserId,
        pagination: Pagination
    ): PaginatedResult<Order> = dbQuery {
        val total = OrdersTable
            .selectAll()
            .where { OrdersTable.userId eq userId.value }
            .count()

        val orderRows = OrdersTable
            .selectAll()
            .where { OrdersTable.userId eq userId.value }
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .toList()

        val orderIds = orderRows.map { it[OrdersTable.id].value }
        val itemsByOrderId = loadOrderItems(orderIds)

        val orders = orderRows.map { row ->
            row.toOrder(itemsByOrderId[row[OrdersTable.id].value] ?: emptyList())
        }

        PaginatedResult(orders, total, pagination.page, pagination.size)
    }

    override suspend fun findAll(
        pagination: Pagination,
        status: OrderStatus?
    ): PaginatedResult<Order> = dbQuery {
        val baseQuery = OrdersTable.selectAll()
        val filteredQuery = status?.let {
            baseQuery.where { OrdersTable.status eq it.name }
        } ?: baseQuery

        val total = filteredQuery.count()

        val orderRows = (status?.let {
            OrdersTable.selectAll().where { OrdersTable.status eq it.name }
        } ?: OrdersTable.selectAll())
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .toList()

        val orderIds = orderRows.map { it[OrdersTable.id].value }
        val itemsByOrderId = loadOrderItems(orderIds)

        val orders = orderRows.map { row ->
            row.toOrder(itemsByOrderId[row[OrdersTable.id].value] ?: emptyList())
        }

        PaginatedResult(orders, total, pagination.page, pagination.size)
    }

    override suspend fun create(order: Order): Order = dbQuery {
        val orderId = UUID.randomUUID()

        OrdersTable.insert {
            it[id] = orderId
            it[userId] = order.userId.value
            it[status] = order.status.name
            it[totalAmount] = order.totalAmount.amount
            it[createdAt] = order.createdAt.toJavaInstant().atOffset(java.time.ZoneOffset.UTC)
            it[updatedAt] = order.updatedAt.toJavaInstant().atOffset(java.time.ZoneOffset.UTC)
        }

        order.items.forEach { item ->
            OrderItemsTable.insert {
                it[id] = UUID.randomUUID()
                it[OrderItemsTable.orderId] = orderId
                it[productId] = item.productId.value
                it[productName] = item.productName
                it[quantity] = item.quantity
                it[pricePerUnit] = item.pricePerUnit.amount
                it[createdAt] = order.createdAt.toJavaInstant().atOffset(java.time.ZoneOffset.UTC)
            }
        }

        findById(OrderId(orderId))!!
    }

    override suspend fun update(order: Order): Order = dbQuery {
        OrdersTable.update({ OrdersTable.id eq order.id.value }) {
            it[status] = order.status.name
            it[totalAmount] = order.totalAmount.amount
        }
        order
    }

    override suspend fun delete(id: OrderId): Boolean = dbQuery {
        OrdersTable.deleteWhere { OrdersTable.id eq id.value } > 0
    }

    private fun loadOrderItems(orderIds: List<UUID>): Map<UUID, List<OrderItem>> {
        if (orderIds.isEmpty()) return emptyMap()

        return OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.orderId inList orderIds }
            .groupBy { it[OrderItemsTable.orderId].value }
            .mapValues { (_, rows) -> rows.map { it.toOrderItem() } }
    }

    private fun ResultRow.toOrder(items: List<OrderItem>) = Order(
        id = OrderId(this[OrdersTable.id].value),
        userId = UserId(this[OrdersTable.userId].value),
        status = OrderStatus.valueOf(this[OrdersTable.status]),
        items = items,
        createdAt = this[OrdersTable.createdAt].toInstant().toKotlinInstant(),
        updatedAt = this[OrdersTable.updatedAt].toInstant().toKotlinInstant()
    )

    private fun ResultRow.toOrderItem() = OrderItem(
        id = OrderItemId(this[OrderItemsTable.id].value),
        productId = ProductId(this[OrderItemsTable.productId].value),
        productName = this[OrderItemsTable.productName],
        quantity = this[OrderItemsTable.quantity],
        pricePerUnit = Money(this[OrderItemsTable.pricePerUnit])
    )
}
