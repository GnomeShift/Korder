package service

import config.DatabaseContext
import exception.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import model.*
import repository.*
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository,
    private val userRepository: UserRepository,
    private val db: DatabaseContext
) : OrderService {
    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 50L
    }

    override suspend fun createOrder(userId: UserId, command: CreateOrderCommand): Result<Order> {
        return createOrderWithRetry(userId, command, MAX_RETRIES)
    }

    private suspend fun createOrderWithRetry(
        userId: UserId,
        command: CreateOrderCommand,
        retriesLeft: Int
    ): Result<Order> = runCatching {
        db.transaction {
            logger.info { "Creating order for user $userId" }

            // Validate customer existence
            val user = userRepository.findById(userId)
                ?: throw EntityNotFoundException("User", userId.toString())

            if (!user.isActive) {
                throw BusinessRuleViolationException("User account deactivated")
            }

            // Load and validate products
            val productIds = command.items.map { it.productId }
            val products = productIds.mapNotNull { productRepository.findById(it) }

            if (products.size != productIds.size) {
                val foundIds = products.map { it.id }
                val missingIds = productIds.filter { it !in foundIds }
                throw EntityNotFoundException("Products", missingIds.joinToString())
            }

            val productMap = products.associateBy { it.id }

            // Load and validate stock
            val stockMap = stockRepository.findByProductIds(productIds)
            val updatedStocks = mutableListOf<Stock>()

            for (item in command.items) {
                val stock = stockMap[item.productId]
                    ?: throw EntityNotFoundException("Stock", item.productId.toString())

                if (!stock.canReserve(item.quantity)) {
                    throw InsufficientStockException(
                        productId = item.productId.toString(),
                        requested = item.quantity,
                        available = stock.availableQuantity
                    )
                }

                updatedStocks.add(stock.reserve(item.quantity))
            }

            // Reserve stock
            stockRepository.updateBatch(updatedStocks)
            logger.debug { "Reserved stock for ${updatedStocks.size} products" }

            // Create order
            val now = Clock.System.now()
            val orderItems = command.items.map { item ->
                val product = productMap[item.productId]!!
                OrderItem(
                    id = OrderItemId.generate(),
                    productId = item.productId,
                    productName = product.name,
                    quantity = item.quantity,
                    pricePerUnit = product.price
                )
            }

            val order = Order(
                id = OrderId.generate(),
                userId = userId,
                status = OrderStatus.PENDING,
                items = orderItems,
                createdAt = now,
                updatedAt = now
            )

            val createdOrder = orderRepository.create(order)

            logger.info {
                "Created order ${createdOrder.id} with ${createdOrder.totalItems} items, total: ${createdOrder.totalAmount.toDecimal()}"
            }

            createdOrder
        }
    }.recoverCatching { exception ->
        if (exception is ConcurrentModificationException && retriesLeft > 0) {
            logger.warn { "Retry due to concurrent modification, retries left: $retriesLeft" }

            delay(RETRY_DELAY_MS * (MAX_RETRIES - retriesLeft + 1))
            createOrderWithRetry(userId, command, retriesLeft - 1).getOrThrow()
        }
        else {
            throw exception
        }
    }

    override suspend fun cancelOrder(userId: UserId, orderId: OrderId): Result<Order> = runCatching {
        db.transaction {
            logger.info { "User $userId cancelling order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.toString())

            // Check for user ownership
            if (order.userId != userId) {
                throw AuthorizationException("You don't have permission")
            }

            if (!order.canCancel()) {
                throw InvalidOrderStateException(
                    orderId = orderId.toString(),
                    currentStatus = order.status.name,
                    action = "cancel"
                )
            }

            // Release reserved stock
            releaseStock(order)

            val cancelledOrder = order.cancel()
            val updatedOrder = orderRepository.update(cancelledOrder)

            logger.info { "Order $orderId cancelled successfully" }

            updatedOrder
        }
    }

    override suspend fun deleteOrder(userId: UserId, orderId: OrderId): Result<Boolean> = runCatching {
        db.transaction {
            logger.info { "User $userId deleting order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.toString())

            // Check for user ownership
            if (order.userId != userId) {
                throw AuthorizationException("You don't have permission")
            }

            // Allow deletion only cancelled or finished orders
            if (order.status !in listOf(OrderStatus.CANCELLED, OrderStatus.COMPLETED)) {
                // If order is active - cancel firstly
                if (order.canCancel()) {
                    releaseStock(order)
                } else {
                    throw InvalidOrderStateException(
                        orderId = orderId.toString(),
                        currentStatus = order.status.name,
                        action = "delete"
                    )
                }
            }

            val deleted = orderRepository.delete(orderId)

            if (deleted) {
                logger.info { "Order $orderId deleted successfully" }
            }

            deleted
        }
    }

    override suspend fun confirmOrder(orderId: OrderId): Result<Order> = runCatching {
        db.transaction {
            logger.info { "Confirming order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.toString())

            if (!order.canConfirm()) {
                throw InvalidOrderStateException(
                    orderId = orderId.toString(),
                    currentStatus = order.status.name,
                    action = "confirm"
                )
            }

            // Convert reservation to actual reduction
            val productIds = order.items.map { it.productId }
            val stockMap = stockRepository.findByProductIds(productIds)

            val updatedStocks = order.items.map { item ->
                val stock = stockMap[item.productId]
                    ?: throw EntityNotFoundException("Stock", item.productId.toString())
                stock.confirmReservation(item.quantity)
            }

            stockRepository.updateBatch(updatedStocks)

            val confirmedOrder = order.confirm()
            val updatedOrder = orderRepository.update(confirmedOrder)

            logger.info { "Order $orderId confirmed successfully" }

            updatedOrder
        }
    }

    override suspend fun getOrder(userId: UserId, orderId: OrderId): Order? {
        val order = orderRepository.findById(orderId) ?: return null

        // User can only see their own orders
        if (order.userId != userId) {
            throw AuthorizationException("You don't have permission")
        }

        return order
    }

    override suspend fun getAllOrders(
        pagination: Pagination,
        status: OrderStatus?
    ): PaginatedResult<Order> {
        return orderRepository.findAll(pagination, status)
    }

    override suspend fun getUserOrders(
        userId: UserId,
        pagination: Pagination
    ): PaginatedResult<Order> {
        return orderRepository.findByUserId(userId, pagination)
    }

    private suspend fun releaseStock(order: Order) {
        val productIds = order.items.map { it.productId }
        val stockMap = stockRepository.findByProductIds(productIds)

        val updatedStocks = order.items.map { item ->
            val stock = stockMap[item.productId]
                ?: throw EntityNotFoundException("Stock", item.productId.toString())

            when (order.status) {
                OrderStatus.PENDING -> stock.cancelReservation(item.quantity)
                OrderStatus.CONFIRMED -> stock.addStock(item.quantity)
                else -> stock
            }
        }

        stockRepository.updateBatch(updatedStocks)
        logger.debug { "Released stock for order ${order.id}" }
    }
}
