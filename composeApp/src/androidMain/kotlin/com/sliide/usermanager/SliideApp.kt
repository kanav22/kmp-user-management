package com.sliide.usermanager

import android.app.Application
import com.sliide.usermanager.di.androidModule
import com.sliide.usermanager.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SliideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin(
            additionalModules = listOf(androidModule),
            appDeclaration = {
                androidContext(this@SliideApp)
                androidLogger()
            }
        )
    }
}
