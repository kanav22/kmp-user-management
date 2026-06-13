package com.sliide.usermanager.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS does not have an Android-style hardware back button.
    // Navigation back is handled via swipe gestures which are managed by the iOS host.
}
