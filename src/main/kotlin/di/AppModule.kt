package di

import config.AppConfig
import config.DatabaseContext
import config.DatabaseFactory
import io.ktor.server.application.*
import org.koin.dsl.module
import persistence.repository.*
import repository.*
import service.*

fun appModule(environment: ApplicationEnvironment) = module {
    // Config
    single { AppConfig.load(environment) }
    single { get<AppConfig>().database }
    single { get<AppConfig>().jwt }

    // Database
    single { DatabaseFactory(get()) }
    single { DatabaseContext() }

    // Repositories
    single<ProductRepository> { ExposedProductRepository(get()) }
    single<StockRepository> { ExposedStockRepository(get()) }
    single<OrderRepository> { ExposedOrderRepository(get()) }
    single<CategoryRepository> { ExposedCategoryRepository(get()) }
    single<UserRepository> { ExposedUserRepository(get()) }

    // Cache
    single { UserCache(get()) }

    // Services
    single<ProductService> { ProductServiceImpl(get(), get(), get()) }
    single<OrderService> { OrderServiceImpl(get(), get(), get(), get(), get()) }
    single<AuthService> { AuthServiceImpl(get(), get()) }
}
