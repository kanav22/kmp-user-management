package com.kanav.usermanager.di

import com.kanav.usermanager.config.goRestToken
import com.kanav.usermanager.data.local.UserLocalDataSource
import com.kanav.usermanager.data.remote.UserApiService
import com.kanav.usermanager.data.repository.UserRepositoryImpl
import com.kanav.usermanager.domain.repository.UserRepository
import com.kanav.usermanager.domain.usecase.AddUserUseCase
import com.kanav.usermanager.domain.usecase.AddUserUseCaseImpl
import com.kanav.usermanager.domain.usecase.DeleteUserUseCase
import com.kanav.usermanager.domain.usecase.DeleteUserUseCaseImpl
import com.kanav.usermanager.domain.usecase.GetLastPageUsersUseCase
import com.kanav.usermanager.domain.usecase.GetLastPageUsersUseCaseImpl
import com.kanav.usermanager.domain.usecase.ObserveUsersUseCase
import com.kanav.usermanager.domain.usecase.ObserveUsersUseCaseImpl
import com.kanav.usermanager.data.local.db.UserDatabase
import com.kanav.usermanager.presentation.userlist.UserListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(get<HttpClientEngine>()) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer $goRestToken")
            }
        }
    }
    single { UserApiService(get()) }
}

val databaseModule = module {
    single { UserDatabase(get()) }
    single { UserLocalDataSource(get()) }
}

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

val domainModule = module {
    single<ObserveUsersUseCase>     { ObserveUsersUseCaseImpl(get()) }
    single<GetLastPageUsersUseCase> { GetLastPageUsersUseCaseImpl(get()) }
    single<AddUserUseCase>          { AddUserUseCaseImpl(get()) }
    single<DeleteUserUseCase>       { DeleteUserUseCaseImpl(get()) }
}

val presentationModule = module {
    viewModel { UserListViewModel(get(), get(), get(), get()) }
}

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: org.koin.core.KoinApplication.() -> Unit = {},
) {
    org.koin.core.context.startKoin {
        appDeclaration()
        modules(networkModule, databaseModule, repositoryModule, domainModule, presentationModule)
        modules(additionalModules)
    }
}
