package persistence.repository

import config.dbQuery
import model.Category
import model.CategoryId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import persistence.table.CategoriesTable
import repository.CategoryRepository
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.toJavaInstant

class ExposedCategoryRepository : CategoryRepository {
    override suspend fun findById(id: CategoryId): Category? = dbQuery {
        CategoriesTable
            .selectAll()
            .where { CategoriesTable.id eq id.value }
            .map { it.toCategory() }
            .singleOrNull()
    }

    override suspend fun findAll(): List<Category> = dbQuery {
        CategoriesTable
            .selectAll()
            .orderBy(CategoriesTable.name to SortOrder.ASC)
            .map { it.toCategory() }
    }

    override suspend fun create(name: String, description: String?): Category = dbQuery {
        val now = Clock.System.now()
        val categoryId = UUID.randomUUID()

        CategoriesTable.insert {
            it[id] = categoryId
            it[CategoriesTable.name] = name
            it[CategoriesTable.description] = description
            it[createdAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
            it[updatedAt] = now.toJavaInstant().atOffset(ZoneOffset.UTC)
        }

        findById(CategoryId(categoryId))!!
    }

    private fun ResultRow.toCategory() = Category(
        id = CategoryId(this[CategoriesTable.id].value),
        name = this[CategoriesTable.name],
        description = this[CategoriesTable.description]
    )
}
