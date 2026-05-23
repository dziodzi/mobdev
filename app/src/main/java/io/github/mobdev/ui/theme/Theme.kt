package io.github.mobdev.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Палитра — тёплый коралл + графит, не дефолтный Material
private val Coral = Color(0xFFFF6B6B)
private val CoralDim = Color(0xFFE05757)
private val Graphite = Color(0xFF1A1A1F)
private val GraphiteSoft = Color(0xFF252530)
private val GraphiteSurface = Color(0xFF2D2D38)
private val Ivory = Color(0xFFF5F1EA)
private val IvorySoft = Color(0xFFEDE7DC)
private val InkBlack = Color(0xFF12121A)
private val Muted = Color(0xFF8E8E9C)

private val DarkPalette = darkColorScheme(
    primary = Coral,
    onPrimary = InkBlack,
    primaryContainer = CoralDim,
    onPrimaryContainer = Ivory,
    secondary = Ivory,
    onSecondary = InkBlack,
    background = Graphite,
    onBackground = Ivory,
    surface = GraphiteSoft,
    onSurface = Ivory,
    surfaceVariant = GraphiteSurface,
    onSurfaceVariant = IvorySoft,
    outline = Muted,
    error = Coral,
    onError = InkBlack
)

private val LightPalette = lightColorScheme(
    primary = CoralDim,
    onPrimary = Ivory,
    primaryContainer = Color(0xFFFFE0E0),
    onPrimaryContainer = Color(0xFF3A0808),
    secondary = Graphite,
    onSecondary = Ivory,
    background = Ivory,
    onBackground = InkBlack,
    surface = Color(0xFFFFFCF5),
    onSurface = InkBlack,
    surfaceVariant = IvorySoft,
    onSurfaceVariant = Graphite,
    outline = Muted,
    error = CoralDim,
    onError = Ivory
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
)

@Composable
fun KekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}