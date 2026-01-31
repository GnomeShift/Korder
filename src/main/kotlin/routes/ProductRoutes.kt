package routes

import dto.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.ProductId
import repository.Pagination
import service.ProductService
import java.util.*

fun Route.productRoutes(productService: ProductService) {
    route("/api/v1/products") {
        get {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val size = call.parameters["size"]?.toIntOrNull() ?: 20
            val query = call.parameters["q"]

            val pagination = Pagination(page, size)

            val result = if (!query.isNullOrBlank()) {
                productService.searchProducts(query, pagination)
            }
            else {
                productService.listProducts(pagination)
            }

            call.respond(ProductListResponse(
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

        get("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid product id")
                )

            val product = productService.getProduct(ProductId(id))
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Not Found", "Product with id $id not found")
                )

            call.respond(product.toResponse())
        }

        post {
            val request = call.receive<CreateProductRequest>()
            val product = productService.createProduct(request.toCommand())
            call.respond(HttpStatusCode.Created, product.toResponse())
        }

        patch("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid product id")
                )

            val request = call.receive<UpdateProductRequest>()
            val product = productService.updateProduct(ProductId(id), request.toCommand())
                ?: return@patch call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Not Found", "Product with id $id not found")
                )

            call.respond(product.toResponse())
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid product id")
                )

            productService.deleteProduct(ProductId(id))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

// Safe UUID parse
private fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
