package routes

import dto.CreateCustomerRequest
import dto.ErrorResponse
import dto.toResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.CustomerId
import repository.CustomerRepository
import java.util.*

fun Route.customerRoutes(customerRepository: CustomerRepository) {
    route("/api/v1/customers") {
        post {
            val request = call.receive<CreateCustomerRequest>()
            val customer = customerRepository.create(request.toCommand())
            call.respond(HttpStatusCode.Created, customer.toResponse())
        }

        get("{id}") {
            val id = call.parameters["id"]?.toUUIDOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Bad Request", "Invalid customer id")
                )

            val customer = customerRepository.findById(CustomerId(id))
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Not Found", "Customer with id $id not found")
                )

            call.respond(customer.toResponse())
        }
    }
}

// Safe UUID parse
private fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
