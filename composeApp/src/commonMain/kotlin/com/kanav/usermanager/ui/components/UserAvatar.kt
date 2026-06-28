package com.kanav.usermanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val avatarColors = listOf(
    Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00695C),
    Color(0xFFE65100), Color(0xFF283593), Color(0xFF880E4F),
    Color(0xFF1B5E20), Color(0xFF4E342E),
)

@Composable
fun UserAvatar(name: String, modifier: Modifier = Modifier) {
    val initial = remember(name) { name.firstOrNull()?.uppercaseChar()?.toString() ?: "?" }
    val bg = remember(name) { avatarColors[name.hashCode().and(0x7fffffff) % avatarColors.size] }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .semantics { contentDescription = "$name's avatar" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
