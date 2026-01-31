package service

import model.Product
import model.ProductId
import model.Stock
import repository.CreateProductCommand
import repository.PaginatedResult
import repository.Pagination
import repository.UpdateProductCommand

interface ProductService {
    suspend fun getProduct(id: ProductId): ProductWithStock?
    suspend fun listProducts(pagination: Pagination): PaginatedResult<ProductWithStock>
    suspend fun searchProducts(query: String, pagination: Pagination): PaginatedResult<ProductWithStock>
    suspend fun createProduct(command: CreateProductCommand): ProductWithStock
    suspend fun updateProduct(id: ProductId, command: UpdateProductCommand): ProductWithStock?
    suspend fun deleteProduct(id: ProductId): Boolean
}

data class ProductWithStock(
    val product: Product,
    val stock: Stock
) {
    val availableQuantity: Int get() = stock.availableQuantity
    val inStock: Boolean get() = availableQuantity > 0
}
