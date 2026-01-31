package di

import persistence.repository.ExposedCategoryRepository
import persistence.repository.ExposedCustomerRepository
import config.AppConfig
import config.DatabaseFactory
import repository.StockRepository
import io.ktor.server.application.*
import org.koin.dsl.module
import persistence.UnitOfWork
import persistence.repository.*
import repository.CategoryRepository
import repository.CustomerRepository
import repository.OrderRepository
import repository.ProductRepository
import service.OrderService
import service.OrderServiceImpl
import service.ProductService
import service.ProductServiceImpl

fun appModule(environment: ApplicationEnvironment) = module {
    // Config
    single { AppConfig.load(environment) }
    single { get<AppConfig>().database }

    // Database
    single { DatabaseFactory(get()) }
    single { UnitOfWork() }

    // Repositories
    single<ProductRepository> { ExposedProductRepository() }
    single<StockRepository> { ExposedStockRepository() }
    single<OrderRepository> { ExposedOrderRepository() }
    single<CustomerRepository> { ExposedCustomerRepository() }
    single<CategoryRepository> { ExposedCategoryRepository() }

    // Services
    single<ProductService> { ProductServiceImpl(get(), get()) }
    single<OrderService> { OrderServiceImpl(get(), get(), get(), get(), get()) }
}
