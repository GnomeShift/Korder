package model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
@JvmInline
value class ProductId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): ProductId = ProductId(UUID.randomUUID())
        fun fromString(value: String): ProductId = ProductId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class CategoryId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): CategoryId = CategoryId(UUID.randomUUID())
        fun fromString(value: String): CategoryId = CategoryId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class CustomerId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): CustomerId = CustomerId(UUID.randomUUID())
        fun fromString(value: String): CustomerId = CustomerId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class OrderId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID())
        fun fromString(value: String): OrderId = OrderId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class OrderItemId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): OrderItemId = OrderItemId(UUID.randomUUID())
        fun fromString(value: String): OrderItemId = OrderItemId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class StockId(@Serializable(with = UUIDSerializer::class) val value: UUID) {
    companion object {
        fun generate(): StockId = StockId(UUID.randomUUID())
        fun fromString(value: String): StockId = StockId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class Money(val amount: Long) {
    init {
        require(amount >= 0) { "Money cannot be negative" }
    }

    operator fun plus(other: Money) = Money(amount + other.amount)
    operator fun times(quantity: Int) = Money(amount * quantity)

    fun toDecimal(): Double = amount / 100.0

    companion object {
        fun fromDecimal(value: Double): Money = Money((value * 100).toLong())
        val ZERO = Money(0)
    }
}

@JvmInline
value class Email(val value: String) {
    init {
        require(EMAIL_REGEX.matches(value)) { "Invalid email format: $value" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
