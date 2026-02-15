package routes

import dto.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.OrderId
import model.OrderStatus
import plugins.adminOnly
import plugins.authenticated
import plugins.requireUser
import repository.Pagination
import service.OrderService
import utils.toUUIDOrNull

fun Route.orderRoutes(orderService: OrderService) {
    route("/api/v1/orders") {
        authenticated {
            get {
                val user = call.requireUser()
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20

                val result = orderService.getUserOrders(user.userId, Pagination(page, size))

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
                val user = call.requireUser()
                val request = call.receive<CreateOrderRequest>()

                orderService.createOrder(user.userId, request.toCommand())
                    .onSuccess { order -> call.respond(HttpStatusCode.Created, order.toResponse()) }
                    .onFailure { error -> throw error }
            }

            get("{id}") {
                val user = call.requireUser()
                val id = call.parameters["id"]?.toUUIDOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Bad Request", "Invalid order id")
                    )

                val order = orderService.getOrder(user.userId, OrderId(id))
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Not Found", "Order with id $id not found")
                    )

                call.respond(order.toResponse())
            }

            delete("{id}") {
                val user = call.requireUser()
                val id = call.parameters["id"]?.toUUIDOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Bad Request", "Invalid order id")
                    )

                orderService.deleteOrder(user.userId, OrderId(id))
                    .onSuccess { call.respond(HttpStatusCode.NoContent) }
                    .onFailure { error -> throw error }
            }

            post("{id}/cancel") {
                val user = call.requireUser()
                val id = call.parameters["id"]?.toUUIDOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Bad Request", "Invalid order id")
                    )

                orderService.cancelOrder(user.userId, OrderId(id))
                    .onSuccess { order -> call.respond(order.toResponse()) }
                    .onFailure { error -> throw error }
            }
        }

        adminOnly {
            get("/all") {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20
                val status = call.parameters["status"]?.let {
                    runCatching { OrderStatus.valueOf(it.uppercase()) }.getOrNull()
                }

                val result = orderService.getAllOrders(
                    Pagination(page, size),
                    status
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
    }
}
