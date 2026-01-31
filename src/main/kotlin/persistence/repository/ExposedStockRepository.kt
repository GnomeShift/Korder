package persistence.repository

import config.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import persistence.table.StockTable
import kotlin.collections.map
import repository.StockRepository
import model.ProductId
import model.Stock
import model.StockId

class ExposedStockRepository : StockRepository {
    override suspend fun findByProductId(productId: ProductId): Stock? = dbQuery {
        StockTable
            .selectAll()
            .where { StockTable.productId eq productId.value }
            .map { it.toStock() }
            .singleOrNull()
    }

    override suspend fun findByProductIds(productIds: List<ProductId>): Map<ProductId, Stock> = dbQuery {
        if (productIds.isEmpty()) return@dbQuery emptyMap()

        StockTable
            .selectAll()
            .where { StockTable.productId inList productIds.map { it.value } }
            .associate { row ->
                ProductId(row[StockTable.productId].value) to row.toStock()
            }
    }

    override suspend fun update(stock: Stock): Stock = dbQuery {
        StockTable.update({ StockTable.id eq stock.id.value }) {
            it[quantity] = stock.quantity
            it[reservedQuantity] = stock.reservedQuantity
        }
        stock
    }

    override suspend fun updateBatch(stocks: List<Stock>): List<Stock> = dbQuery {
        stocks.forEach { stock ->
            StockTable.update({ StockTable.id eq stock.id.value }) {
                it[quantity] = stock.quantity
                it[reservedQuantity] = stock.reservedQuantity
            }
        }
        stocks
    }

    private fun ResultRow.toStock() = Stock(
        id = StockId(this[StockTable.id].value),
        productId = ProductId(this[StockTable.productId].value),
        quantity = this[StockTable.quantity],
        reservedQuantity = this[StockTable.reservedQuantity]
    )
}
