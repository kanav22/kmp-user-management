package com.sliide.usermanager.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for JVM desktop — back navigation handled at the window level
}
