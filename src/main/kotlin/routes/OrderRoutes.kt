package routes

import dto.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.CustomerId
import model.OrderId
import model.OrderStatus
import repository.Pagination
import service.OrderService
import java.util.*

fun Route.orderRoutes(orderService: OrderService) {
    route("/api/v1/orders") {
        get {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val size = call.parameters["size"]?.toIntOrNull() ?: 20
            val status = call.parameters["status"]?.let {
                runCatching { OrderStatus.valueOf(it.uppercase()) }.getOrNull()
            }

            val pagination = Pagination(page, size)
            val result = orderService.getAllOrders(pagination, status)

            call.respond(OrderListResponse(
                items = result.items.map { it.toResponse() },
                pagination = PaginationResponse(
                    page = result.page,
                    size = result.size,
                    total = result.total,
                    totalPages = result.totalPages,
                    hasNext = result.hasNext,
                    hasPrevious = result.hasPrevious
                )
            ))
        }

        post {
            val request = call.receive<CreateOrderRequest>()

            orderService.createOrder(request.toCommand())
                .onSuccess { order -> call.respond(HttpStatusCode.Created, order.toResponse()) }
                .onFailure { error -> throw error }
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid order id")
                )

            val order = orderService.getOrder(OrderId(id))
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Not Found", "Order with id $id not found")
                )

            call.respond(order.toResponse())
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid order id")
                )

            orderService.deleteOrder(OrderId(id))
                .onSuccess { call.respond(HttpStatusCode.NoContent) }
                .onFailure { error -> throw error }
        }

        post("{id}/cancel") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid order id")
                )

            orderService.cancelOrder(OrderId(id))
                .onSuccess { order -> call.respond(order.toResponse()) }
                .onFailure { error -> throw error }
        }

        post("{id}/confirm") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid order id")
                )

            orderService.confirmOrder(OrderId(id))
                .onSuccess { order -> call.respond(order.toResponse()) }
                .onFailure { error -> throw error }
        }
    }

    route("/api/v1/customers/{customerId}/orders") {
        get {
            val customerId = call.parameters["customerId"]?.toUUIDOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid customer id")
                )

            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val size = call.parameters["size"]?.toIntOrNull() ?: 20

            val result = orderService.getCustomerOrders(
                CustomerId(customerId),
                Pagination(page, size)
            )

            call.respond(OrderListResponse(
                items = result.items.map { it.toResponse() },
                pagination = PaginationResponse(
                    page = result.page,
                    size = result.size,
                    total = result.total,
                    totalPages = result.totalPages,
                    hasNext = result.hasNext,
                    hasPrevious = result.hasPrevious
                )
            ))
        }
    }
}

// Safe UUID parse
private fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
