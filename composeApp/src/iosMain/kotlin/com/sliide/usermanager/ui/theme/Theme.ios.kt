package com.sliide.usermanager.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) = AppThemeBase(darkTheme = darkTheme, content = content)
