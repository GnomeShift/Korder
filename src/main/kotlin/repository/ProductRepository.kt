package repository

import model.CategoryId
import model.Money
import model.Product
import model.ProductId

interface ProductRepository {
    suspend fun findById(id: ProductId): Product?
    suspend fun findAll(pagination: Pagination): PaginatedResult<Product>
    suspend fun findByCategory(categoryId: CategoryId, pagination: Pagination): PaginatedResult<Product>
    suspend fun search(query: String, pagination: Pagination): PaginatedResult<Product>
    suspend fun create(command: CreateProductCommand): Product
    suspend fun update(id: ProductId, command: UpdateProductCommand): Product?
    suspend fun delete(id: ProductId): Boolean
}

data class CreateProductCommand(
    val name: String,
    val description: String,
    val price: Money,
    val categoryId: CategoryId,
    val initialStock: Int
)

data class UpdateProductCommand(
    val name: String? = null,
    val description: String? = null,
    val price: Money? = null,
    val categoryId: CategoryId? = null
) {
    fun hasUpdates(): Boolean = name != null || description != null || price != null || categoryId != null
}

data class Pagination(
    val page: Int = 1,
    val size: Int = 20
) {
    val offset: Int get() = (page - 1) * size

    init {
        require(page >= 1) { "Page must be >= 1" }
        require(size in 1..100) { "Size must be between 1 and 100" }
    }
}

data class PaginatedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int
) {
    val totalPages: Int get() = ((total + size - 1) / size).toInt()
    val hasNext: Boolean get() = page < totalPages
    val hasPrevious: Boolean get() = page > 1
}
