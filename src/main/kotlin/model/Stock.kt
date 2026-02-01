package model

data class Stock(
    val id: StockId,
    val productId: ProductId,
    val quantity: Int,
    val reservedQuantity: Int,
    val version: Long = 0
) {
    val availableQuantity: Int get() = quantity - reservedQuantity

    fun canReserve(amount: Int): Boolean = availableQuantity >= amount

    fun reserve(amount: Int): Stock {
        require(canReserve(amount)) { "Cannot reserve $amount items. Available: $availableQuantity" }
        return copy(reservedQuantity = reservedQuantity + amount)
    }

    fun cancelReservation(amount: Int): Stock {
        require(reservedQuantity >= amount) { "Cannot cancel reservation of $amount. Reserved: $reservedQuantity" }
        return copy(reservedQuantity = reservedQuantity - amount)
    }

    fun confirmReservation(amount: Int): Stock {
        require(reservedQuantity >= amount) { "Cannot confirm reservation of $amount. Reserved: $reservedQuantity" }
        return copy(
            quantity = quantity - amount,
            reservedQuantity = reservedQuantity - amount
        )
    }

    fun addStock(amount: Int): Stock {
        require(amount > 0) { "Amount must be positive" }
        return copy(quantity = quantity + amount)
    }
}
