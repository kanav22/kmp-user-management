package com.sliide.usermanager.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.sliide.usermanager.data.local.db.SliideDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<SqlDriver>        { AndroidSqliteDriver(SliideDatabase.Schema, androidContext(), "sliide.db") }
    single<HttpClientEngine> { OkHttp.create() }
}
