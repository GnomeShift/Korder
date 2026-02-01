package persistence.repository

import config.DatabaseContext
import model.ProductId
import model.Stock
import model.StockId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import persistence.table.StockTable
import repository.StockRepository
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class ExposedStockRepository(
    private val db: DatabaseContext
) : StockRepository {
    override suspend fun findByProductId(productId: ProductId): Stock? = db.query {
        StockTable
            .selectAll()
            .where { StockTable.productId eq productId.value }
            .map { it.toStock() }
            .singleOrNull()
    }

    override suspend fun findByProductIds(productIds: List<ProductId>): Map<ProductId, Stock> = db.query {
        if (productIds.isEmpty()) return@query emptyMap()
        StockTable
            .selectAll()
            .where { StockTable.productId inList productIds.map { it.value } }
            .associate { ProductId(it[StockTable.productId].value) to it.toStock() }
    }

    override suspend fun update(stock: Stock): Stock = db.query {
        val now = Clock.System.now()

        val updatedRows = StockTable.update({
            (StockTable.id eq stock.id.value) and (StockTable.version eq stock.version)
        }) {
            it[quantity] = stock.quantity
            it[reservedQuantity] = stock.reservedQuantity
            it[version] = stock.version + 1
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        if (updatedRows == 0) {
            throw ConcurrentModificationException("Stock ${stock.id} modified concurrently")
        }

        stock.copy(version = stock.version + 1)
    }

    override suspend fun updateBatch(stocks: List<Stock>): List<Stock> = db.query {
        val now = Clock.System.now()

        stocks.map { stock ->
            val updatedRows = StockTable.update({
                (StockTable.id eq stock.id.value) and (StockTable.version eq stock.version)
            }) {
                it[quantity] = stock.quantity
                it[reservedQuantity] = stock.reservedQuantity
                it[version] = stock.version + 1
                it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
            }

            if (updatedRows == 0) {
                throw ConcurrentModificationException("Stock ${stock.id} modified concurrently")
            }

            stock.copy(version = stock.version + 1)
        }
    }

    private fun ResultRow.toStock() = Stock(
        id = StockId(this[StockTable.id].value),
        productId = ProductId(this[StockTable.productId].value),
        quantity = this[StockTable.quantity],
        reservedQuantity = this[StockTable.reservedQuantity],
        version = this[StockTable.version]
    )
}
