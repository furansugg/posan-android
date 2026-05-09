package com.posan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.posan.app.ui.theme.CatBlue
import com.posan.app.ui.theme.CatCyan
import com.posan.app.ui.theme.CatGreen
import com.posan.app.ui.theme.CatOrange
import com.posan.app.ui.theme.CatPink
import com.posan.app.ui.theme.CatPurple
import com.posan.app.ui.theme.CatRose
import com.posan.app.ui.theme.CatTeal

private val AvatarPalette = listOf(CatBlue, CatTeal, CatOrange, CatPink, CatPurple, CatRose, CatGreen, CatCyan)

fun avatarColorFor(seed: String): Color {
    if (seed.isEmpty()) return AvatarPalette[0]
    val hash = seed.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFFF }
    return AvatarPalette[hash % AvatarPalette.size]
}

@Composable
fun Avatar(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = avatarColorFor(text)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
