package service

import exception.BusinessRuleViolationException
import exception.EntityNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import model.Product
import model.ProductId
import repository.*

private val logger = KotlinLogging.logger {}

class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository,
    private val categoryRepository: CategoryRepository
) : ProductService {
    override suspend fun getProduct(id: ProductId): ProductWithStock? {
        val product = productRepository.findById(id) ?: return null
        val stock = stockRepository.findByProductId(id)
            ?: throw IllegalStateException("Stock not found for product ${id.value}")

        return ProductWithStock(product, stock)
    }

    override suspend fun listProducts(pagination: Pagination): PaginatedResult<ProductWithStock> {
        val products = productRepository.findAll(pagination)
        return enrichWithStock(products)
    }

    override suspend fun searchProducts(
        query: String,
        pagination: Pagination
    ): PaginatedResult<ProductWithStock> {
        val products = productRepository.search(query, pagination)
        return enrichWithStock(products)
    }

    override suspend fun createProduct(command: CreateProductCommand): ProductWithStock {
        logger.info { "Creating product: ${command.name}" }

        val product = productRepository.create(command)
        val stock = stockRepository.findByProductId(product.id)
            ?: throw IllegalStateException("Stock not created for product ${product.id.value}")

        logger.info { "Created product ${product.id.value} with ${stock.quantity} items in stock" }

        return ProductWithStock(product, stock)
    }

    override suspend fun updateProduct(id: ProductId, command: UpdateProductCommand): ProductWithStock? {
        logger.info { "Updating product: ${id.value}" }

        val updatedProduct = productRepository.update(id, command) ?: return null
        val stock = stockRepository.findByProductId(id)
            ?: throw IllegalStateException("Stock not found for product ${id.value}")

        logger.info { "Updated product ${id.value}" }

        return ProductWithStock(updatedProduct, stock)
    }

    override suspend fun deleteProduct(id: ProductId): Boolean {
        logger.info { "Deleting product: ${id.value}" }

        // Check for product existence
        productRepository.findById(id)
            ?: throw EntityNotFoundException("Product", id.value)

        // Check for existing reservation
        val stock = stockRepository.findByProductId(id)
        if (stock != null && stock.reservedQuantity > 0) {
            throw BusinessRuleViolationException(
                "Cannot delete product ${id.value}: has ${stock.reservedQuantity} reserved items"
            )
        }

        val deleted = productRepository.delete(id)

        if (deleted) {
            logger.info { "Deleted product ${id.value}" }
        }

        return deleted
    }

    private suspend fun enrichWithStock(
        products: PaginatedResult<Product>
    ): PaginatedResult<ProductWithStock> {
        val productIds = products.items.map { it.id }
        val stockMap = stockRepository.findByProductIds(productIds)

        val itemsWithStock = products.items.map { product ->
            val stock = stockMap[product.id]
                ?: throw IllegalStateException("Stock not found for product ${product.id.value}")
            ProductWithStock(product, stock)
        }

        return PaginatedResult(
            items = itemsWithStock,
            total = products.total,
            page = products.page,
            size = products.size
        )
    }
}
