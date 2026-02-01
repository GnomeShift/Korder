package config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*

private val logger = KotlinLogging.logger {}

data class AppConfig(
    val environment: Environment,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val admin: AdminConfig,
    val cors: CorsConfig,
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
            val profile = EnvLoader.get("APP_ENV", "production")!!
            logger.info { "Loading configuration for profile: $profile" }

            return AppConfig(
                environment = Environment.fromString(profile),
                database = DatabaseConfig.fromConfig(config),
                jwt = JwtConfig.fromEnv(),
                admin = AdminConfig.fromEnv(),
                cors = CorsConfig.fromEnv(),
                logging = LoggingConfig.fromEnv()
            )
        }
    }
}

data class AdminConfig(
    val email: String,
    val password: String
) {
    companion object {
        fun fromEnv() = AdminConfig(
            email = EnvLoader.getRequired("ADMIN_EMAIL"),
            password = EnvLoader.getRequired("ADMIN_PASSWORD")
        )
    }
}

data class CorsConfig(
    val allowedHosts: List<String>,
    val allowCredentials: Boolean
) {
    companion object {
        fun fromEnv() = CorsConfig(
            allowedHosts = EnvLoader.get("CORS_ALLOWED_HOSTS", "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            allowCredentials = EnvLoader.getBoolean("CORS_ALLOW_CREDENTIALS", true)
        )
    }
}

data class LoggingConfig(val level: String) {
    companion object {
        fun fromEnv() = LoggingConfig(
            level = EnvLoader.get("LOG_LEVEL", "INFO")!!
        )
    }
}
