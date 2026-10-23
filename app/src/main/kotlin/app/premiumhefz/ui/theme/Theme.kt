package app.premiumhefz.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.premiumhefz.data.ThemeMode

@Immutable
data class BrandColors(val gold: Color, val goldSoft: Color, val hairline: Color)

val LocalBrandColors = staticCompositionLocalOf { BrandColors(Gold500, Gold300, Gold500.copy(alpha = 0.35f)) }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

val MaterialTheme.brand: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current

private val DarkColors = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color.White,
    primaryContainer = Emerald700,
    onPrimaryContainer = Emerald100,
    secondary = Gold400,
    onSecondary = Emerald950,
    secondaryContainer = Color(0xFF3A3318),
    onSecondaryContainer = Gold100,
    tertiary = Gold300,
    onTertiary = Emerald950,
    background = Emerald900,
    onBackground = Color(0xFFE6EFE9),
    surface = Emerald900,
    onSurface = Color(0xFFE6EFE9),
    surfaceVariant = Emerald800,
    onSurfaceVariant = Color(0xFFA6BDB2),
    surfaceContainerLowest = Emerald950,
    surfaceContainerLow = Color(0xFF062A20),
    surfaceContainer = Emerald850,
    surfaceContainerHigh = Emerald800,
    surfaceContainerHighest = Emerald750,
    outline = Color(0xFF4A6E5F),
    outlineVariant = Color(0xFF244538),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Color(0xFF3A2C05),
    tertiary = Gold700,
    onTertiary = Color.White,
    background = Ivory50,
    onBackground = Ink900,
    surface = Ivory50,
    onSurface = Ink900,
    surfaceVariant = Ivory200,
    onSurfaceVariant = Color(0xFF4B5A52),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F3E6),
    surfaceContainer = Ivory100,
    surfaceContainerHigh = Color(0xFFEFE9D7),
    surfaceContainerHighest = Ivory200,
    outline = Color(0xFF86948B),
    outlineVariant = Color(0xFFD6D0BC),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PremiumHefzTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val brand = if (dark) {
        BrandColors(Gold400, Gold300, Gold400.copy(alpha = 0.30f))
    } else {
        BrandColors(Gold600, Gold700, Gold600.copy(alpha = 0.35f))
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalBrandColors provides brand, LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
