package dev.goelo.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Gold = Color(0xFFE5C689)
val MutedGold = Color(0xFFAD9160)
val Ink = Color(0xFF0D100F)
val Panel = Color(0xFF171C19)
val Positive = Color(0xFF90CCAD)
val Negative = Color(0xFFE49B90)

private val Palette = darkColorScheme(
    primary = Gold, onPrimary = Color(0xFF262012),
    primaryContainer = Color(0xFF3B3424), onPrimaryContainer = Color(0xFFF4DFB4),
    secondary = Positive, onSecondary = Ink,
    secondaryContainer = Color(0xFF263C30), onSecondaryContainer = Positive,
    tertiary = Gold, background = Ink, onBackground = Color(0xFFF4F2EB),
    surface = Panel, onSurface = Color(0xFFF4F2EB),
    surfaceVariant = Color(0xFF232B25), onSurfaceVariant = Color(0xFFA2ABA2),
    surfaceContainer = Panel, surfaceContainerLow = Color(0xFF121713),
    surfaceContainerHigh = Color(0xFF1F2721), surfaceContainerHighest = Color(0xFF29322C),
    outline = Color(0xFF536050), outlineVariant = Color(0xFF30392F),
    error = Negative, onError = Ink,
)
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily=FontFamily.SansSerif, fontSize=size.sp, lineHeight=height.sp, fontWeight=weight)
private val AppType = Typography(
    displayLarge=type(60,68,FontWeight.Light).copy(letterSpacing=(-2).sp),
    displayMedium=type(46,54,FontWeight.Light), displaySmall=type(36,44,FontWeight.Light),
    headlineLarge=type(30,38,FontWeight.SemiBold), headlineMedium=type(26,34,FontWeight.SemiBold),
    headlineSmall=type(23,30,FontWeight.Medium), titleLarge=type(22,30,FontWeight.SemiBold),
    titleMedium=type(16,24,FontWeight.Medium), titleSmall=type(14,22,FontWeight.Medium),
    bodyLarge=type(16,26), bodyMedium=type(14,23), bodySmall=type(12,20),
    labelLarge=type(14,20,FontWeight.SemiBold), labelMedium=type(12,18,FontWeight.Medium),
    labelSmall=type(11,16,FontWeight.Medium),
)
@Composable fun BlackGoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=Palette, typography=AppType,
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp), small=RoundedCornerShape(12.dp),
            medium=RoundedCornerShape(18.dp), large=RoundedCornerShape(24.dp), extraLarge=RoundedCornerShape(30.dp)),
        content=content)
}
