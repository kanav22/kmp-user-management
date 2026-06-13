package com.sliide.usermanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.usermanager.di.initKoinIos
import com.sliide.usermanager.ui.screens.UserListScreen
import com.sliide.usermanager.ui.theme.AppTheme

fun MainViewController() = ComposeUIViewController(
    configure = { initKoinIos() }
) {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        UserListScreen()
    }
}
