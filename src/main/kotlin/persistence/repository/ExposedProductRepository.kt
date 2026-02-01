package persistence.repository

import config.dbQuery
import model.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import persistence.table.CategoriesTable
import persistence.table.ProductsTable
import persistence.table.StockTable
import repository.*
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

class ExposedProductRepository : ProductRepository {
    override suspend fun findById(id: ProductId): Product? = dbQuery {
        (ProductsTable innerJoin CategoriesTable)
            .selectAll()
            .where { ProductsTable.id eq id.value }
            .map { it.toProduct() }
            .singleOrNull()
    }

    override suspend fun findAll(pagination: Pagination): PaginatedResult<Product> = dbQuery {
        val total = ProductsTable.selectAll().count()

        val items = (ProductsTable innerJoin CategoriesTable)
            .selectAll()
            .orderBy(ProductsTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .map { it.toProduct() }

        PaginatedResult(items, total, pagination.page, pagination.size)
    }

    override suspend fun findByCategory(
        categoryId: CategoryId,
        pagination: Pagination
    ): PaginatedResult<Product> = dbQuery {
        val total = ProductsTable
            .selectAll()
            .where { ProductsTable.categoryId eq categoryId.value }
            .count()

        val items = (ProductsTable innerJoin CategoriesTable)
            .selectAll()
            .where { ProductsTable.categoryId eq categoryId.value }
            .orderBy(ProductsTable.createdAt to SortOrder.DESC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .map { it.toProduct() }

        PaginatedResult(items, total, pagination.page, pagination.size)
    }

    override suspend fun search(query: String, pagination: Pagination): PaginatedResult<Product> = dbQuery {
        val searchPattern = "%${query.lowercase()}%"

        val searchCondition: Op<Boolean> =
            (ProductsTable.name.lowerCase() like searchPattern) or
                    (ProductsTable.description.lowerCase() like searchPattern)

        val total = ProductsTable.selectAll()
            .where { searchCondition }
            .count()

        val items = (ProductsTable innerJoin CategoriesTable)
            .selectAll()
            .where { searchCondition }
            .orderBy(ProductsTable.name to SortOrder.ASC)
            .limit(pagination.size)
            .offset(pagination.offset.toLong())
            .map { it.toProduct() }

        PaginatedResult(items, total, pagination.page, pagination.size)
    }

    override suspend fun create(command: CreateProductCommand): Product = dbQuery {
        val now = Clock.System.now()
        val productId = UUID.randomUUID()

        ProductsTable.insert {
            it[id] = productId
            it[name] = command.name
            it[description] = command.description
            it[price] = command.price.amount
            it[categoryId] = command.categoryId.value
            it[createdAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        // Create initial stock
        StockTable.insert {
            it[id] = UUID.randomUUID()
            it[StockTable.productId] = productId
            it[quantity] = command.initialStock
            it[reservedQuantity] = 0
            it[version] = 0
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        findById(ProductId(productId))!!
    }

    override suspend fun update(id: ProductId, command: UpdateProductCommand): Product? = dbQuery {
        if (!command.hasUpdates()) return@dbQuery findById(id)

        val updated = ProductsTable.update({ ProductsTable.id eq id.value }) { stmt ->
            command.name?.let { stmt[name] = it }
            command.description?.let { stmt[description] = it }
            command.price?.let { stmt[price] = it.amount }
            command.categoryId?.let { stmt[categoryId] = it.value }
            stmt[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        if (updated > 0) findById(id) else null
    }

    override suspend fun delete(id: ProductId): Boolean = dbQuery {
        ProductsTable.deleteWhere { ProductsTable.id eq id.value } > 0
    }

    private fun ResultRow.toProduct(): Product {
        val category = Category(
            id = CategoryId(this[CategoriesTable.id].value),
            name = this[CategoriesTable.name],
            description = this[CategoriesTable.description]
        )

        return Product(
            id = ProductId(this[ProductsTable.id].value),
            name = this[ProductsTable.name],
            description = this[ProductsTable.description],
            price = Money(this[ProductsTable.price]),
            category = category,
            createdAt = this[ProductsTable.createdAt].toInstant().toKotlinInstant(),
            updatedAt = this[ProductsTable.updatedAt].toInstant().toKotlinInstant()
        )
    }
}
