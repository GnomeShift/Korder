package routes

import dto.CategoryResponse
import io.ktor.server.response.*
import io.ktor.server.routing.*
import repository.CategoryRepository

fun Route.categoryRoutes(categoryRepository: CategoryRepository) {
    route("/api/v1/categories") {
        get {
            val categories = categoryRepository.findAll()
            call.respond(categories.map {
                CategoryResponse(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description
                )
            })
        }
    }
}
