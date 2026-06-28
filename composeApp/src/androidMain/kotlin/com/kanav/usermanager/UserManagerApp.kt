package com.kanav.usermanager

import android.app.Application
import com.kanav.usermanager.di.androidModule
import com.kanav.usermanager.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class UserManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin(
            additionalModules = listOf(androidModule),
            appDeclaration = {
                androidContext(this@UserManagerApp)
                androidLogger()
            }
        )
    }
}
