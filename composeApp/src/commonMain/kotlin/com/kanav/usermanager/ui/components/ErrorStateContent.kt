package com.kanav.usermanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kanav.usermanager.domain.repository.UserListError

@Composable
fun ErrorStateContent(
    error: UserListError?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val icon = when (error) {
        UserListError.NoInternet   -> Icons.Filled.Warning
        UserListError.MissingToken -> Icons.Filled.Info
        else                       -> Icons.Filled.Warning
    }
    val title = when (error) {
        UserListError.NoInternet   -> "No internet connection"
        UserListError.MissingToken -> "API token not configured"
        else                       -> "Something went wrong"
    }
    val body = when (error) {
        UserListError.NoInternet   -> "Check your network and try again."
        UserListError.MissingToken -> "See README → Token Setup for instructions. A rebuild is required after adding the token."
        else                       -> "An unexpected error occurred. Please try again."
    }

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (onRetry != null && error != UserListError.MissingToken) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f)) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun InlineErrorBanner(error: UserListError?, modifier: Modifier = Modifier) {
    if (error == null) return
    val message = when (error) {
        UserListError.NoInternet   -> "No internet — showing cached data"
        UserListError.MissingToken -> "API token not configured. See README."
        else                       -> "Refresh failed — showing cached data"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
