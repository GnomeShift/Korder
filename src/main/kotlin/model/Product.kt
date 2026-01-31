package model

import kotlin.time.Instant

data class Product(
    val id: ProductId,
    val name: String,
    val description: String,
    val price: Money,
    val category: Category,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Category(
    val id: CategoryId,
    val name: String,
    val description: String?
)
