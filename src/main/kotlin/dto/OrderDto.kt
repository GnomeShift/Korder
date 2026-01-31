package dto

import kotlinx.serialization.Serializable
import model.CustomerId
import model.Order
import model.OrderItem
import model.ProductId
import service.CreateOrderCommand
import service.OrderItemRequest

@Serializable
data class OrderResponse(
    val id: String,
    val customerId: String,
    val status: String,
    val items: List<OrderItemResponse>,
    val totalAmount: Double,
    val totalItems: Int,
    val canCancel: Boolean,
    val canConfirm: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class OrderItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val subtotal: Double
)

@Serializable
data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItemRequestDto>
) {
    fun toCommand() = CreateOrderCommand(
        customerId = CustomerId.fromString(customerId),
        items = items.map {
            OrderItemRequest(
                ProductId.fromString(it.productId),
                it.quantity
            )
        }
    )
}

@Serializable
data class OrderItemRequestDto(
    val productId: String,
    val quantity: Int
)

@Serializable
data class OrderListResponse(
    val items: List<OrderResponse>,
    val pagination: PaginationResponse
)

fun Order.toResponse() = OrderResponse(
    id = id.toString(),
    customerId = customerId.toString(),
    status = status.name,
    items = items.map { it.toResponse() },
    totalAmount = totalAmount.toDecimal(),
    totalItems = totalItems,
    canCancel = canCancel(),
    canConfirm = canConfirm(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

fun OrderItem.toResponse() = OrderItemResponse(
    id = id.toString(),
    productId = productId.toString(),
    productName = productName,
    quantity = quantity,
    pricePerUnit = pricePerUnit.toDecimal(),
    subtotal = subtotal.toDecimal()
)
