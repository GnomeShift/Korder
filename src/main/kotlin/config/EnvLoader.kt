package config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

object EnvLoader {
    private lateinit var dotenv: Dotenv

    fun load(profile: String? = null): Dotenv {
        val envFile = when {
            profile != null -> ".env.$profile"
            else -> ".env"
        }

        dotenv = dotenv {
            directory = "./"
            filename = envFile
            ignoreIfMalformed = true
            ignoreIfMissing = true
            systemProperties = true
        }

        logger.info { "Env loaded: $envFile" }
        logger.info { "Active profile: ${get("APP_ENV", "production")}" }

        return dotenv
    }

    fun get(key: String, default: String? = null): String? {
        return System.getenv(key) ?: if (::dotenv.isInitialized) dotenv[key] ?: default else default
    }

    fun getRequired(key: String): String {
        return get(key) ?: throw IllegalStateException("Environment variable '$key' isn't set")
    }

    fun getInt(key: String, default: Int): Int {
        return get(key)?.toIntOrNull() ?: default
    }

    fun getLong(key: String, default: Long): Long {
        return get(key)?.toLongOrNull() ?: default
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return get(key)?.lowercase()?.toBooleanStrictOrNull() ?: default
    }
}
