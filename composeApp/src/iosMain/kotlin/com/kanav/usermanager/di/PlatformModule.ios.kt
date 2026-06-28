package com.kanav.usermanager.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.kanav.usermanager.data.local.db.UserDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

val iosModule = module {
    single<SqlDriver>        { NativeSqliteDriver(UserDatabase.Schema, "usermanager.db") }
    single<HttpClientEngine> { Darwin.create() }
}

fun initKoinIos() = initKoin(listOf(iosModule))
