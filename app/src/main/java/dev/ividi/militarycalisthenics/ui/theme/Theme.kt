package dev.ividi.militarycalisthenics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iVidi.dev brand palette — used everywhere, no dynamic/system colors.
val BgBase = Color(0xFF0A0A0F)
val BgPanel = Color(0xFF0D0D18)
val BgPanel2 = Color(0xFF12121F)
val AccentOrange = Color(0xFFF99C00)
val AccentYellow = Color(0xFFFCBB00)
val AccentDark = Color(0xFFDD7400)
val TextPrimary = Color(0xFFE2E8F0)
val TextDim = Color(0xFF94A3B8)
val TextFaint = Color(0xFF5B6474)
val ColorError = Color(0xFFEF4444)
val ColorOk = Color(0xFF22C55E)

private val MilitaryColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = BgBase,
    secondary = AccentYellow,
    onSecondary = BgBase,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgPanel,
    onSurface = TextPrimary,
    surfaceVariant = BgPanel2,
    onSurfaceVariant = TextDim,
    error = ColorError,
    outline = TextFaint
)

@Composable
fun MilitaryCalisthenicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MilitaryColorScheme,
        typography = MilitaryTypography,
        content = content
    )
}
