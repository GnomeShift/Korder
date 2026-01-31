package di

import config.AppConfig
import config.DatabaseFactory
import io.ktor.server.application.*
import org.koin.dsl.module
import persistence.UnitOfWork
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
    single { UnitOfWork() }

    // Repositories
    single<ProductRepository> { ExposedProductRepository() }
    single<StockRepository> { ExposedStockRepository() }
    single<OrderRepository> { ExposedOrderRepository() }
    single<CategoryRepository> { ExposedCategoryRepository() }
    single<UserRepository> { ExposedUserRepository() }

    // Services
    single<ProductService> { ProductServiceImpl(get(), get()) }
    single<OrderService> { OrderServiceImpl(get(), get(), get(), get(), get()) }
    single<AuthService> { AuthServiceImpl(get(), get()) }
}
