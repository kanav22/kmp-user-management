package com.sliide.usermanager.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.sliide.usermanager.data.local.db.SliideDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

val iosModule = module {
    single<SqlDriver>        { NativeSqliteDriver(SliideDatabase.Schema, "sliide.db") }
    single<HttpClientEngine> { Darwin.create() }
}

fun initKoinIos() = initKoin(listOf(iosModule))
