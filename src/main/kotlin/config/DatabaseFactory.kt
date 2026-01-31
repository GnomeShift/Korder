package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

class DatabaseFactory(private val config: DatabaseConfig) {
    private lateinit var dataSource: HikariDataSource

    fun connect(): Database {
        logger.info { "Connecting to database: ${config.jdbcUrl}" }

        dataSource = createHikariDataSource()
        runMigrations(dataSource)

        return Database.connect(dataSource).also {
            logger.info { "Database connection successful" }
        }
    }

    fun close() {
        if (::dataSource.isInitialized && !dataSource.isClosed) {
            dataSource.close()
            logger.info { "Database connection pool closed" }
        }
    }

    private fun createHikariDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            driverClassName = config.driver
            username = config.user
            password = config.password
            maximumPoolSize = config.pool.maxSize
            minimumIdle = config.pool.minIdle
            idleTimeout = config.pool.idleTimeout
            connectionTimeout = config.pool.connectionTimeout
            maxLifetime = config.pool.maxLifetime
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Query optimization
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }

        return HikariDataSource(hikariConfig).also {
            logger.info { "HikariCP connection pool created (max: ${config.pool.maxSize})" }
        }
    }

    private fun runMigrations(dataSource: DataSource) {
        logger.info { "Running migration..." }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateMigrationNaming(true)
            .cleanDisabled(true)
            .load()

        val result = flyway.migrate()

        logger.info {
            "${result.migrationsExecuted} migrations applied, current version: ${result.targetSchemaVersion}"
        }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    suspendTransaction { withContext(Dispatchers.IO) { block() } }