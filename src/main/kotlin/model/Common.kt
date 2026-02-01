package model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID
import kotlin.math.roundToLong

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
        require(amount <= MAX_AMOUNT) { "Money exceeds maximum allowed value" }
    }

    operator fun plus(other: Money): Money {
        val sum = amount + other.amount
        check(sum in amount..MAX_AMOUNT) { "Money overflow" }
        return Money(sum)
    }

    operator fun times(quantity: Int): Money {
        require(quantity >= 0) { "Quantity cannot be negative" }
        val result = amount.toBigInteger() * quantity.toBigInteger()
        check(result <= MAX_AMOUNT.toBigInteger()) { "Money overflow" }
        return Money(result.toLong())
    }

    fun toDecimal(): Double = amount / 100.0

    companion object {
        const val MAX_AMOUNT = 999_999_999_999L
        val ZERO = Money(0)

        fun fromDecimal(value: Double): Money {
            require(value >= 0) { "Money cannot be negative" }
            require(value <= MAX_AMOUNT / 100.0) { "Money exceeds maximum allowed value" }
            return Money((value * 100).roundToLong())
        }
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
