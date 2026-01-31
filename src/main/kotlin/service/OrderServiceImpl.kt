package service

import exception.EntityNotFoundException
import exception.InsufficientStockException
import exception.InvalidOrderStateException
import io.github.oshai.kotlinlogging.KotlinLogging
import model.CustomerId
import model.Order
import model.OrderId
import model.OrderItem
import model.OrderItemId
import model.OrderStatus
import model.Stock
import persistence.UnitOfWork
import repository.CustomerRepository
import repository.OrderRepository
import repository.PaginatedResult
import repository.Pagination
import repository.ProductRepository
import repository.StockRepository
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository,
    private val customerRepository: CustomerRepository,
    private val unitOfWork: UnitOfWork
) : OrderService {
    override suspend fun createOrder(command: CreateOrderCommand): Result<Order> = runCatching {
        unitOfWork.transaction {
            logger.info { "Creating order for customer ${command.customerId}" }

            // Validate customer existence
            customerRepository.findById(command.customerId)
                ?: throw EntityNotFoundException("Customer", command.customerId.toString())

            // Load and validate products
            val productIds = command.items.map { it.productId }
            val products = productIds.mapNotNull { productRepository.findById(it) }

            if (products.size != productIds.size) {
                val foundIds = products.map { it.id }
                val missingIds = productIds.filter { it !in foundIds }
                throw EntityNotFoundException("Products", missingIds.map { it.toString() })
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
                customerId = command.customerId,
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
    }

    override suspend fun cancelOrder(orderId: OrderId): Result<Order> = runCatching {
        unitOfWork.transaction {
            logger.info { "Cancelling order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.value.toString())

            if (!order.canCancel()) {
                throw InvalidOrderStateException(
                    orderId = orderId.value.toString(),
                    currentStatus = order.status.name,
                    action = "cancel"
                )
            }

            // Release reserved stock
            releaseStock(order)

            val cancelledOrder = order.cancel()
            orderRepository.update(cancelledOrder)

            logger.info { "Order $orderId cancelled successfully" }

            cancelledOrder
        }
    }

    override suspend fun deleteOrder(orderId: OrderId): Result<Boolean> = runCatching {
        unitOfWork.transaction {
            logger.info { "Deleting order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.value.toString())

            // Allow to delete only cancelled or finished orders
            if (order.status !in listOf(OrderStatus.CANCELLED, OrderStatus.COMPLETED)) {
                // If order is active - cancel firstly
                if (order.canCancel()) {
                    releaseStock(order)
                } else {
                    throw InvalidOrderStateException(
                        orderId = orderId.value.toString(),
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
        unitOfWork.transaction {
            logger.info { "Confirming order $orderId" }

            val order = orderRepository.findById(orderId)
                ?: throw EntityNotFoundException("Order", orderId.value.toString())

            if (!order.canConfirm()) {
                throw InvalidOrderStateException(
                    orderId = orderId.value.toString(),
                    currentStatus = order.status.name,
                    action = "confirm"
                )
            }

            // Convert reservation to actual reduction
            val productIds = order.items.map { it.productId }
            val stockMap = stockRepository.findByProductIds(productIds)

            val updatedStocks = order.items.map { item ->
                val stock = stockMap[item.productId]
                    ?: throw EntityNotFoundException("Stock", item.productId.value.toString())
                stock.confirmReservation(item.quantity)
            }

            stockRepository.updateBatch(updatedStocks)

            val confirmedOrder = order.confirm()
            orderRepository.update(confirmedOrder)

            logger.info { "Order $orderId confirmed successfully" }

            confirmedOrder
        }
    }

    override suspend fun getOrder(orderId: OrderId): Order? {
        return orderRepository.findById(orderId)
    }

    override suspend fun getAllOrders(
        pagination: Pagination,
        status: OrderStatus?
    ): PaginatedResult<Order> {
        return orderRepository.findAll(pagination, status)
    }

    override suspend fun getCustomerOrders(
        customerId: CustomerId,
        pagination: Pagination
    ): PaginatedResult<Order> {
        return orderRepository.findByCustomerId(customerId, pagination)
    }

    private suspend fun releaseStock(order: Order) {
        val productIds = order.items.map { it.productId }
        val stockMap = stockRepository.findByProductIds(productIds)

        val updatedStocks = order.items.map { item ->
            val stock = stockMap[item.productId]
                ?: throw EntityNotFoundException("Stock", item.productId.value.toString())

            when (order.status) {
                OrderStatus.PENDING -> stock.cancelReservation(item.quantity)
                OrderStatus.CONFIRMED -> stock.copy(quantity = stock.quantity + item.quantity)
                else -> stock
            }
        }

        stockRepository.updateBatch(updatedStocks)
        logger.debug { "Released stock for order ${order.id.value}" }
    }
}
