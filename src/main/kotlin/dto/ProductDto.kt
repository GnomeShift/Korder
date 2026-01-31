package dto

import kotlinx.serialization.Serializable
import model.CategoryId
import model.Money
import repository.CreateProductCommand
import repository.UpdateProductCommand
import service.ProductWithStock

@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: CategoryResponse,
    val availableQuantity: Int,
    val inStock: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val description: String?
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Double,
    val categoryId: String,
    val initialStock: Int = 0
) {
    fun toCommand() = CreateProductCommand(
        name = name,
        description = description,
        price = Money.fromDecimal(price),
        categoryId = CategoryId.fromString(categoryId),
        initialStock = initialStock
    )
}

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val categoryId: String? = null
) {
    fun toCommand() = UpdateProductCommand(
        name = name,
        description = description,
        price = price?.let { Money.fromDecimal(it) },
        categoryId = categoryId?.let { CategoryId.fromString(it) }
    )
}

@Serializable
data class ProductListResponse(
    val items: List<ProductResponse>,
    val pagination: PaginationResponse
)

@Serializable
data class PaginationResponse(
    val page: Int,
    val size: Int,
    val total: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

fun ProductWithStock.toResponse() = ProductResponse(
    id = product.id.toString(),
    name = product.name,
    description = product.description,
    price = product.price.toDecimal(),
    category = CategoryResponse(
        id = product.category.id.toString(),
        name = product.category.name,
        description = product.category.description
    ),
    availableQuantity = availableQuantity,
    inStock = inStock,
    createdAt = product.createdAt.toString(),
    updatedAt = product.updatedAt.toString()
)
