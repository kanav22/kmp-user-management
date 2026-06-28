package com.kanav.usermanager.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.kanav.usermanager.data.local.db.UserDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<SqlDriver>        { AndroidSqliteDriver(UserDatabase.Schema, androidContext(), "usermanager.db") }
    single<HttpClientEngine> { OkHttp.create() }
}
