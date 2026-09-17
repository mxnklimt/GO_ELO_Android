package dev.goelo.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackGoldColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD9BD78),
    onPrimary = Color(0xFF11110F),
    background = Color(0xFF11110F),
    onBackground = Color(0xFFF4F0E7),
    surface = Color(0xFF1B1B18),
    onSurface = Color(0xFFF4F0E7),
    onSurfaceVariant = Color(0xFFAAA79B),
)

@Composable
fun BlackGoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackGoldColorScheme,
        content = content,
    )
}
