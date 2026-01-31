package service

import config.JwtConfig
import exception.BusinessRuleViolationException
import io.github.oshai.kotlinlogging.KotlinLogging
import model.Email
import model.User
import model.UserId
import model.UserRole
import org.mindrot.jbcrypt.BCrypt
import repository.CreateUserCommand
import repository.UserRepository
import javax.naming.AuthenticationException

private val logger = KotlinLogging.logger {}

interface AuthService {
    suspend fun register(command: RegisterCommand): AuthResult
    suspend fun login(command: LoginCommand): AuthResult
    suspend fun findUserById(id: UserId): User?
    suspend fun checkAdminExistence(email: String, password: String)
}

data class RegisterCommand(
    val email: Email,
    val password: String,
    val firstName: String,
    val lastName: String
) {
    init {
        require(password.length >= 8) { "Password must be at least 8 characters" }
    }
}

data class LoginCommand(
    val email: Email,
    val password: String
)

data class AuthResult(
    val user: User,
    val token: String
)

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) : AuthService {
    override suspend fun register(command: RegisterCommand): AuthResult {
        logger.info { "Registering new user: ${command.email.value}" }

        // Check for email existence
        if (userRepository.existsByEmail(command.email)) {
            throw BusinessRuleViolationException("Email ${command.email.value} is already registered")
        }

        // Hash password
        val passwordHash = BCrypt.hashpw(command.password, BCrypt.gensalt(12))

        // Create user
        val user = userRepository.create(
            CreateUserCommand(
                email = command.email,
                passwordHash = passwordHash,
                firstName = command.firstName,
                lastName = command.lastName,
                role = UserRole.USER
            )
        )

        logger.info { "User registered: ${user.id}" }

        // Generate token
        val token = jwtConfig.generateToken(user.id, user.email.value, user.role)

        return AuthResult(user, token)
    }

    override suspend fun login(command: LoginCommand): AuthResult {
        logger.info { "Login attempt for: ${command.email.value}" }

        // Find user
        val user = userRepository.findByEmail(command.email)
            ?: throw AuthenticationException("Invalid email or password")

        // Check if user is active
        if (!user.isActive) {
            throw AuthenticationException("Account deactivated")
        }

        // Verify password
        if (!BCrypt.checkpw(command.password, user.passwordHash)) {
            logger.warn { "Failed login attempt for: ${command.email.value}" }
            throw AuthenticationException("Invalid email or password")
        }

        logger.info { "User logged in: ${user.id}" }

        // Generate token
        val token = jwtConfig.generateToken(user.id, user.email.value, user.role)

        return AuthResult(user, token)
    }

    override suspend fun findUserById(id: UserId): User? {
        return userRepository.findById(id)
    }

    override suspend fun checkAdminExistence(email: String, password: String) {
        val adminEmail = Email(email)

        if (!userRepository.existsByEmail(adminEmail)) {
            val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12))

            userRepository.create(
                CreateUserCommand(
                    email = adminEmail,
                    passwordHash = passwordHash,
                    firstName = "Admin",
                    lastName = "Admin",
                    role = UserRole.ADMIN
                )
            )

            logger.info { "First admin created" }
        }
    }
}
