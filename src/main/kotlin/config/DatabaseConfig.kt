package config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.config.*

private val logger = KotlinLogging.logger {}

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val driver: String,
    val pool: PoolConfig
) {
    val jdbcUrl: String get() = "jdbc:postgresql://$host:$port/$name"

    data class PoolConfig(
        val maxSize: Int,
        val minIdle: Int,
        val idleTimeout: Long,
        val connectionTimeout: Long,
        val maxLifetime: Long
    )

    companion object {
        fun fromConfig(config: ApplicationConfig): DatabaseConfig {
            val dbConfig = config.config("database")

            return DatabaseConfig(
                host = EnvLoader.getRequired("DB_HOST"),
                port = EnvLoader.getInt("DB_PORT", 5432),
                name = EnvLoader.getRequired("DB_NAME"),
                user = EnvLoader.getRequired("DB_USER"),
                password = EnvLoader.getRequired("DB_PASSWORD"),
                driver = dbConfig.propertyOrNull("driver")?.getString() ?: "org.postgresql.Driver",
                pool = PoolConfig(
                    maxSize = EnvLoader.getInt("DB_POOL_MAX_SIZE", 10),
                    minIdle = EnvLoader.getInt("DB_POOL_MIN_IDLE", 2),
                    idleTimeout = dbConfig.propertyOrNull("pool.idleTimeout") ?.getString()?.toLongOrNull() ?: 600000L,
                    connectionTimeout = dbConfig.propertyOrNull("pool.connectionTimeout") ?.getString()?.toLongOrNull() ?: 30000L,
                    maxLifetime = dbConfig.propertyOrNull("pool.maxLifetime") ?.getString()?.toLongOrNull() ?: 1800000L
                )
            ).also {
                logger.info { "Database: ${it.jdbcUrl} (pool: ${it.pool.maxSize})" }
            }
        }
    }
}
