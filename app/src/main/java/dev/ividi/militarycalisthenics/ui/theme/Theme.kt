package dev.ividi.militarycalisthenics.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** iVidi.dev brand palette, in both a dark and a light variant — no dynamic/system colors. */
data class AppColors(
    val bgBase: Color,
    val bgPanel: Color,
    val bgPanel2: Color,
    val accentOrange: Color,
    val accentYellow: Color,
    val accentDark: Color,
    val textPrimary: Color,
    val textDim: Color,
    val textFaint: Color,
    val colorError: Color,
    val colorOk: Color
)

val DarkAppColors = AppColors(
    bgBase = Color(0xFF0A0A0F),
    bgPanel = Color(0xFF0D0D18),
    bgPanel2 = Color(0xFF12121F),
    accentOrange = Color(0xFFF99C00),
    accentYellow = Color(0xFFFCBB00),
    accentDark = Color(0xFFDD7400),
    textPrimary = Color(0xFFE2E8F0),
    textDim = Color(0xFF94A3B8),
    textFaint = Color(0xFF5B6474),
    colorError = Color(0xFFEF4444),
    colorOk = Color(0xFF22C55E)
)

// Warm off-white background, deepened accents for AA contrast on light panels
// (e.g. white-on-#D9760A text still clears ~3:1, and #D9760A-on-#FAF8F5 clears ~3.3:1).
val LightAppColors = AppColors(
    bgBase = Color(0xFFFAF8F5),
    bgPanel = Color(0xFFF1EDE6),
    bgPanel2 = Color(0xFFE8E2D8),
    accentOrange = Color(0xFFD9760A),
    accentYellow = Color(0xFFB8860B),
    accentDark = Color(0xFFA85C00),
    textPrimary = Color(0xFF1A1712),
    textDim = Color(0xFF52493C),
    textFaint = Color(0xFF8A8072),
    colorError = Color(0xFFC22A2A),
    colorOk = Color(0xFF167A3D)
)

val LocalAppColors = compositionLocalOf { DarkAppColors }

val BgBase: Color @Composable get() = LocalAppColors.current.bgBase
val BgPanel: Color @Composable get() = LocalAppColors.current.bgPanel
val BgPanel2: Color @Composable get() = LocalAppColors.current.bgPanel2
val AccentOrange: Color @Composable get() = LocalAppColors.current.accentOrange
val AccentYellow: Color @Composable get() = LocalAppColors.current.accentYellow
val AccentDark: Color @Composable get() = LocalAppColors.current.accentDark
val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextDim: Color @Composable get() = LocalAppColors.current.textDim
val TextFaint: Color @Composable get() = LocalAppColors.current.textFaint
val ColorError: Color @Composable get() = LocalAppColors.current.colorError
val ColorOk: Color @Composable get() = LocalAppColors.current.colorOk

@Composable
fun MilitaryCalisthenicsTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (useDark) DarkAppColors else LightAppColors

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = colors.accentOrange,
            onPrimary = colors.bgBase,
            secondary = colors.accentYellow,
            onSecondary = colors.bgBase,
            background = colors.bgBase,
            onBackground = colors.textPrimary,
            surface = colors.bgPanel,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.bgPanel2,
            onSurfaceVariant = colors.textDim,
            error = colors.colorError,
            outline = colors.textFaint
        )
    } else {
        lightColorScheme(
            primary = colors.accentOrange,
            onPrimary = colors.bgBase,
            secondary = colors.accentYellow,
            onSecondary = colors.bgBase,
            background = colors.bgBase,
            onBackground = colors.textPrimary,
            surface = colors.bgPanel,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.bgPanel2,
            onSurfaceVariant = colors.textDim,
            error = colors.colorError,
            outline = colors.textFaint
        )
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MilitaryTypography,
            content = content
        )
    }
}
