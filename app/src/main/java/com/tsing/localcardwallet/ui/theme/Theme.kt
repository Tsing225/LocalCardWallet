package com.tsing.localcardwallet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tsing.localcardwallet.AppSettings

/** 深色：近乎纯黑的暖灰，让卡片本身发光 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C74FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2A2650),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = Color(0xFF9C99C6),
    onSecondary = Color(0xFF15131F),
    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFECEBF2),
    surface = Color(0xFF14141B),
    onSurface = Color(0xFFECEBF2),
    surfaceVariant = Color(0xFF20202A),
    onSurfaceVariant = Color(0xFF9E9DB0),
    outline = Color(0xFF35353F),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0000),
)

/** 浅色：暖白底 + 靛蓝主色 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5FEF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E4FF),
    onPrimaryContainer = Color(0xFF1A1849),
    secondary = Color(0xFF63618A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF15141C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15141C),
    surfaceVariant = Color(0xFFEDECF5),
    onSurfaceVariant = Color(0xFF5D5C6E),
    outline = Color(0xFFD5D3E2),
    error = Color(0xFFD8453F),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun LocalCardWalletTheme(
    themeMode: Int = AppSettings.themeMode,
    dynamicColor: Boolean = AppSettings.dynamicColor,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
