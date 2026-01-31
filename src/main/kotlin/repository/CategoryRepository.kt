package repository

import model.Category
import model.CategoryId

interface CategoryRepository {
    suspend fun findById(id: CategoryId): Category?
    suspend fun findAll(): List<Category>
    suspend fun create(name: String, description: String?): Category
}
