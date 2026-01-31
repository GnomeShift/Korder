package persistence.table

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object CategoriesTable : UUIDTable("categories") {
    val name = varchar("name", 100).uniqueIndex()
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object ProductsTable : UUIDTable("products") {
    val name = varchar("name", 255)
    val description = text("description")
    val priceAmount = long("price_amount")
    val categoryId = reference("category_id", CategoriesTable)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object StockTable : UUIDTable("stock") {
    val productId = reference("product_id", ProductsTable).uniqueIndex()
    val quantity = integer("quantity")
    val reservedQuantity = integer("reserved_quantity")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object CustomersTable : UUIDTable("customers") {
    val email = varchar("email", 255).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val phone = varchar("phone", 20).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object OrdersTable : UUIDTable("orders") {
    val customerId = reference("customer_id", CustomersTable)
    val status = varchar("status", 20)
    val totalAmount = long("total_amount")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object OrderItemsTable : UUIDTable("order_items") {
    val orderId = reference("order_id", OrdersTable)
    val productId = reference("product_id", ProductsTable)
    val productName = varchar("product_name", 255)
    val quantity = integer("quantity")
    val pricePerUnit = long("price_per_unit")
    val createdAt = timestampWithTimeZone("created_at")
}
