package com.kanav.usermanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanav.usermanager.domain.model.User
import com.kanav.usermanager.domain.util.toRelativeString

@Composable
fun UserDetailPane(user: User?, modifier: Modifier = Modifier) {
    if (user == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Select a user to view details",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        UserAvatar(name = user.name, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(user.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        DetailRow(label = "Status", value = user.status.name.lowercase().replaceFirstChar { it.uppercaseChar() })
        Spacer(Modifier.height(8.dp))
        DetailRow(label = "Gender", value = user.gender.name.lowercase().replaceFirstChar { it.uppercaseChar() })
        Spacer(Modifier.height(8.dp))
        DetailRow(label = "Added", value = user.addedAt.toRelativeString())
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
