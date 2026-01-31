package config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*

private val logger = KotlinLogging.logger {}

data class AppConfig(
    val environment: Environment,
    val database: DatabaseConfig,
    val logging: LoggingConfig
) {
    enum class Environment {
        DEVELOPMENT,
        PRODUCTION,
        TEST;

        companion object {
            fun fromString(value: String): Environment {
                return entries.find { it.name.equals(value, ignoreCase = true) } ?: PRODUCTION
            }
        }
    }

    val isDevelopment: Boolean get() = environment == Environment.DEVELOPMENT

    companion object {
        fun load(environment: ApplicationEnvironment): AppConfig {
            val config = environment.config

            // Set profile
            val profile = EnvLoader.get("APP_ENV", "production")
            logger.info { "Loading configuration for profile: $profile" }

            EnvLoader.load(profile)

            return AppConfig(
                environment = Environment.fromString(profile!!),
                database = DatabaseConfig.fromConfig(config),
                logging = LoggingConfig.fromEnv()
            )
        }
    }
}

data class LoggingConfig(val level: String) {
    companion object {
        fun fromEnv() = LoggingConfig(
            level = EnvLoader.get("LOG_LEVEL", "INFO")!!
        )
    }
}
