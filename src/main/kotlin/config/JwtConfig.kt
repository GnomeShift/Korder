package config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.github.oshai.kotlinlogging.KotlinLogging
import model.UserId
import model.UserRole
import java.util.*

private val logger = KotlinLogging.logger {}

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationHours: Long
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: UserId, email: String, role: UserRole): String {
        val expiresAt = Date(System.currentTimeMillis() + expirationHours * 60 * 60 * 1000)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withExpiresAt(expiresAt)
            .withIssuedAt(Date())
            .sign(algorithm)
            .also {
                logger.debug { "Generated JWT token for user $email with role $role" }
            }
    }

    companion object {
        fun fromEnv(): JwtConfig {
            val secret = EnvLoader.getRequired("JWT_SECRET")

            require(secret.length >= 32) {
                "JWT_SECRET must be at least 32 characters long"
            }

            return JwtConfig(
                secret = secret,
                issuer = EnvLoader.get("JWT_ISSUER", "korder")!!,
                audience = EnvLoader.get("JWT_AUDIENCE", "korder-users")!!,
                realm = EnvLoader.get("JWT_REALM", "korder")!!,
                expirationHours = EnvLoader.getLong("JWT_EXPIRATION_HOURS", 24)
            )
        }
    }
}
