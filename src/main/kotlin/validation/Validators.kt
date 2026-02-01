package validation

import dto.*
import io.ktor.server.plugins.requestvalidation.*
import utils.isValidUUID

object ValidationRules {
    fun notBlank(value: String, field: String): String? =
        if (value.isBlank()) "$field: cannot be blank" else null

    fun maxLength(value: String, max: Int, field: String): String? =
        if (value.length > max) "$field: cannot exceed $max characters" else null

    fun minLength(value: String, min: Int, field: String): String? =
        if (value.length < min) "$field: must be at least $min characters" else null

    fun positive(value: Number, field: String): String? =
        if (value.toDouble() <= 0) "$field: must be positive" else null

    fun nonNegative(value: Number, field: String): String? =
        if (value.toDouble() < 0) "$field: cannot be negative" else null

    fun max(value: Number, max: Number, field: String): String? =
        if (value.toDouble() > max.toDouble()) "$field: cannot exceed $max" else null

    fun validUUID(value: String, field: String): String? =
        if (!value.isValidUUID()) "$field: must be a valid UUID" else null

    fun email(value: String, field: String): String? =
        if (!EMAIL_REGEX.matches(value)) "$field: invalid format" else null

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
}

fun RequestValidationConfig.configureValidation() {
    validate<CreateProductRequest> { request ->
        val errors = listOfNotNull(
            ValidationRules.notBlank(request.name, "name"),
            ValidationRules.maxLength(request.name, 255, "name"),
            ValidationRules.notBlank(request.description, "description"),
            ValidationRules.positive(request.price, "price"),
            ValidationRules.max(request.price, 1_000_000, "price"),
            ValidationRules.validUUID(request.categoryId, "categoryId"),
            ValidationRules.nonNegative(request.initialStock, "initialStock")
        )

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<UpdateProductRequest> { request ->
        val errors = listOfNotNull(
            request.name?.let { ValidationRules.notBlank(it, "name") },
            request.name?.let { ValidationRules.maxLength(it, 255, "name") },
            request.price?.let { ValidationRules.positive(it, "price") },
            request.price?.let { ValidationRules.max(it, 1_000_000, "price") },
            request.categoryId?.let { ValidationRules.validUUID(it, "categoryId") }
        )

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<CreateOrderRequest> { request ->
        val errors = mutableListOf<String>()

        if (request.items.isEmpty()) {
            errors.add("items: order must have at least one item")
        }

        request.items.forEachIndexed { index, item ->
            ValidationRules.validUUID(item.productId, "items[$index].productId")?.let { errors.add(it) }
            ValidationRules.positive(item.quantity, "items[$index].quantity")?.let { errors.add(it) }
            ValidationRules.max(item.quantity, 1000, "items[$index].quantity")?.let { errors.add(it) }
        }

        // Check for duplicate products
        val productIds = request.items.map { it.productId }
        if (productIds.size != productIds.toSet().size) {
            errors.add("items: duplicate products aren't allowed, combine quantities instead")
        }

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<RegisterRequest> { request ->
        val errors = listOfNotNull(
            ValidationRules.notBlank(request.email, "email"),
            ValidationRules.email(request.email, "email"),
            ValidationRules.minLength(request.password, 8, "password"),
            ValidationRules.maxLength(request.password, 100, "password"),
            ValidationRules.notBlank(request.firstName, "firstName"),
            ValidationRules.maxLength(request.firstName, 100, "firstName"),
            ValidationRules.notBlank(request.lastName, "lastName"),
            ValidationRules.maxLength(request.lastName, 100, "lastName")
        )

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    validate<LoginRequest> { request ->
        val errors = listOfNotNull(
            ValidationRules.notBlank(request.email, "email"),
            ValidationRules.notBlank(request.password, "password")
        )

        if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }
}
