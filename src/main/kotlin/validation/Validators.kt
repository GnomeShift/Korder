package validation

import dto.CreateCustomerRequest
import dto.CreateOrderRequest
import dto.CreateProductRequest
import dto.UpdateProductRequest
import io.ktor.server.plugins.requestvalidation.*
import java.util.*

fun RequestValidationConfig.configureValidation() {
    validate<CreateProductRequest> { request ->
        val errors = mutableListOf<String>()

        if (request.name.isBlank()) {
            errors.add("name: cannot be blank")
        }
        if (request.name.length > 255) {
            errors.add("name: cannot exceed 255 characters")
        }
        if (request.description.isBlank()) {
            errors.add("description: cannot be blank")
        }
        if (request.price <= 0) {
            errors.add("price: must be positive")
        }
        if (request.price > 1_000_000) {
            errors.add("price: cannot exceed 1,000,000")
        }
        if (!request.categoryId.isValidUUID()) {
            errors.add("categoryId: must be a valid UUID")
        }
        if (request.initialStock < 0) {
            errors.add("initialStock: cannot be negative")
        }

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<UpdateProductRequest> { request ->
        val errors = mutableListOf<String>()

        request.name?.let {
            if (it.isBlank()) errors.add("name: cannot be blank")
            if (it.length > 255) errors.add("name: cannot exceed 255 characters")
        }
        request.price?.let {
            if (it <= 0) errors.add("price: must be positive")
            if (it > 1_000_000) errors.add("price: cannot exceed 1,000,000")
        }
        request.categoryId?.let {
            if (!it.isValidUUID()) errors.add("categoryId: must be a valid UUID")
        }

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<CreateOrderRequest> { request ->
        val errors = mutableListOf<String>()

        if (!request.customerId.isValidUUID()) {
            errors.add("customerId: must be a valid UUID")
        }
        if (request.items.isEmpty()) {
            errors.add("items: order must have at least one item")
        }

        request.items.forEachIndexed { index, item ->
            if (!item.productId.isValidUUID()) {
                errors.add("items[$index].productId: must be a valid UUID")
            }
            if (item.quantity <= 0) {
                errors.add("items[$index].quantity: must be positive")
            }
            if (item.quantity > 1000) {
                errors.add("items[$index].quantity: cannot exceed 1000")
            }
        }

        // Check for duplicate products
        val productIds = request.items.map { it.productId }
        if (productIds.size != productIds.toSet().size) {
            errors.add("items: duplicate products aren't allowed, combine quantities instead")
        }

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<CreateCustomerRequest> { request ->
        val errors = mutableListOf<String>()

        if (request.email.isBlank()) {
            errors.add("email: cannot be blank")
        }
        if (!request.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            errors.add("email: invalid format")
        }
        if (request.firstName.isBlank()) {
            errors.add("firstName: cannot be blank")
        }
        if (request.firstName.length > 100) {
            errors.add("firstName: cannot exceed 100 characters")
        }
        if (request.lastName.isBlank()) {
            errors.add("lastName: cannot be blank")
        }
        if (request.lastName.length > 100) {
            errors.add("lastName: cannot exceed 100 characters")
        }
        request.phone?.let {
            if (!it.matches(Regex("^\\+?[0-9]{10,15}$"))) {
                errors.add("phone: invalid format")
            }
        }

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }
}

// Safe UUID parse
private fun String.isValidUUID(): Boolean = runCatching { UUID.fromString(this) }.isSuccess
