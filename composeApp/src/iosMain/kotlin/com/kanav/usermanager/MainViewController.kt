package com.kanav.usermanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.kanav.usermanager.di.initKoinIos
import com.kanav.usermanager.ui.screens.UserListScreen
import com.kanav.usermanager.ui.theme.AppTheme

fun MainViewController() = ComposeUIViewController(
    configure = { initKoinIos() }
) {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        UserListScreen()
    }
}
