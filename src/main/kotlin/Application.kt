import config.AppConfig
import config.DatabaseFactory
import config.EnvLoader
import di.appModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import plugins.configureExceptionHandling
import plugins.configureSecurity
import repository.CategoryRepository
import routes.authRoutes
import routes.categoryRoutes
import routes.orderRoutes
import routes.productRoutes
import service.AuthService
import service.OrderService
import service.ProductService
import validation.configureValidation

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    // Load .env before start
    val profile = System.getenv("APP_ENV") ?: "production"
    EnvLoader.load(profile)

    // Start ktor
    EngineMain.main(args)
}

fun Application.module() {
    // DI
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment))
    }

    val appConfig by inject<AppConfig>()
    logger.info { "Starting application in ${appConfig.environment} mode" }

    // Database connection
    val databaseFactory by inject<DatabaseFactory>()
    databaseFactory.connect()

    // Create default admin if doesn't exists
    val authService by inject<AuthService>()
    runBlocking {
        authService.checkAdminExistence(
            email = appConfig.admin.email,
            password = appConfig.admin.password
        )
    }

    // Graceful shutdown
    monitor.subscribe(ApplicationStopped) {
        logger.info { "Application stopping..." }
        databaseFactory.close()
    }

    // Configure plugins
    configureContentNegotiation()
    configureCors(appConfig)
    configureCallLogging()
    configureSecurity(appConfig.jwt, authService)

    install(RequestValidation) {
        configureValidation()
    }

    configureExceptionHandling()

    // Routes
    configureRouting()

    logger.info {
        "Application started successfully on port ${environment.config.port}"
    }
}

private fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

private fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)

        if (config.isDevelopment) {
            anyHost()
            logger.warn { "CORS: development mode" }
        } else {
            val allowedHosts = EnvLoader.get("CORS_ALLOWED_HOSTS", "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            allowedHosts.forEach { _ -> }
        }
    }
}

private fun Application.configureCallLogging() {
    install(CallLogging) {
        filter { call -> call.request.path().startsWith("/api") }

        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            "$method $path -> $status (${duration}ms)"
        }
    }
}

private fun Application.configureRouting() {
    val productService by inject<ProductService>()
    val orderService by inject<OrderService>()
    val categoryRepository by inject<CategoryRepository>()
    val authService by inject<AuthService>()

    routing {
        get("/health") {
            call.respond(mapOf(
                "status" to "UP"
            ))
        }

        // API routes
        authRoutes(authService)
        productRoutes(productService)
        orderRoutes(orderService)
        categoryRoutes(categoryRepository)
    }
}
