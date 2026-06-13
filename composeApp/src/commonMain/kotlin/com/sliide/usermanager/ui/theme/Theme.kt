package com.sliide.usermanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SeedColor,
    onPrimary = OnSeedColor,
    primaryContainer = SeedColorLight,
)

private val DarkColors = darkColorScheme(
    primary = SeedColorLight,
    onPrimary = SeedColorDark,
    primaryContainer = SeedColorDark,
)

@Composable
expect fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
)

@Composable
fun AppThemeBase(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
