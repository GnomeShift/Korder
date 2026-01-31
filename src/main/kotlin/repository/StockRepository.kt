package repository

import model.ProductId
import model.Stock

interface StockRepository {
    suspend fun findByProductId(productId: ProductId): Stock?
    suspend fun findByProductIds(productIds: List<ProductId>): Map<ProductId, Stock>
    suspend fun update(stock: Stock): Stock
    suspend fun updateBatch(stocks: List<Stock>): List<Stock>
}
