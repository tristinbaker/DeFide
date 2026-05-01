package com.tristinbaker.defide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tristinbaker.defide.data.preferences.AppFont
import com.tristinbaker.defide.data.preferences.AppTheme

// Palette
private val Navy         = Color(0xFF1B2B5E)
private val NavyDark     = Color(0xFF111E42)
private val Gold         = Color(0xFFC9993A)
private val GoldLight    = Color(0xFFE8BC6A)
private val Cream        = Color(0xFFF7F3EC)
private val DarkBg       = Color(0xFF0F1520)
private val DarkSurface  = Color(0xFF1A2235)
private val DarkSurface2 = Color(0xFF222D42)

private val LightColors = lightColorScheme(
    primary            = Navy,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFD6DFFF),
    onPrimaryContainer = NavyDark,
    secondary          = Gold,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFFFF0D0),
    onSecondaryContainer = Color(0xFF3D2800),
    background         = Cream,
    onBackground       = Color(0xFF0F1520),
    surface            = Color.White,
    onSurface          = Color(0xFF0F1520),
    surfaceVariant     = Color(0xFFEEE9DF),
    onSurfaceVariant   = Color(0xFF4A4540),
    outline            = Color(0xFFBDB5A8),
)

private val DarkColors = darkColorScheme(
    primary            = GoldLight,
    onPrimary          = Color(0xFF3D2800),
    primaryContainer   = Color(0xFF553A00),
    onPrimaryContainer = GoldLight,
    secondary          = Color(0xFF8BA3D4),
    onSecondary        = Color(0xFF0A1A40),
    secondaryContainer = Color(0xFF1B2B5E),
    onSecondaryContainer = Color(0xFFD0DCFF),
    background         = DarkBg,
    onBackground       = Color(0xFFEAE4D8),
    surface            = DarkSurface,
    onSurface          = Color(0xFFEAE4D8),
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = Color(0xFFB8B0A4),
    outline            = Color(0xFF4A4540),
)

private val AmoledColors = darkColorScheme(
    primary            = GoldLight,
    onPrimary          = Color(0xFF3D2800),
    primaryContainer   = Color(0xFF553A00),
    onPrimaryContainer = GoldLight,
    secondary          = Color(0xFF8BA3D4),
    onSecondary        = Color(0xFF0A1A40),
    secondaryContainer = Color(0xFF0D0D0D),
    onSecondaryContainer = Color(0xFFD0DCFF),
    background         = Color.Black,
    onBackground       = Color(0xFFEAE4D8),
    surface            = Color.Black,
    onSurface          = Color(0xFFEAE4D8),
    surfaceVariant     = Color(0xFF0D0D0D),
    onSurfaceVariant   = Color(0xFFB8B0A4),
    outline            = Color(0xFF2A2A2A),
)

private fun buildTypography(font: AppFont): Typography {
    val display = when (font) {
        AppFont.SERIF      -> FontFamily.Serif
        AppFont.SYSTEM     -> FontFamily.Default
        AppFont.SANS_SERIF -> FontFamily.SansSerif
    }
    val body = when (font) {
        AppFont.SERIF      -> FontFamily.Default
        AppFont.SYSTEM     -> FontFamily.Default
        AppFont.SANS_SERIF -> FontFamily.SansSerif
    }
    return Typography(
        displayLarge  = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold,     fontSize = 57.sp, lineHeight = 64.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium= TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge    = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium   = TextStyle(fontFamily = body,   fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall    = TextStyle(fontFamily = body,   fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge     = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium    = TextStyle(fontFamily = body,   fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.sp),
        bodySmall     = TextStyle(fontFamily = body,   fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge    = TextStyle(fontFamily = body,   fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
        labelMedium   = TextStyle(fontFamily = body,   fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall    = TextStyle(fontFamily = body,   fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
}

@Composable
fun DeFideTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    font: AppFont = AppFont.SERIF,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (theme) {
        AppTheme.LIGHT   -> LightColors
        AppTheme.DARK    -> DarkColors
        AppTheme.AMOLED  -> AmoledColors
        AppTheme.SYSTEM  -> if (isSystemInDarkTheme()) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildTypography(font),
        content = content,
    )
}
