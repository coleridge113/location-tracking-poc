package com.metromart.locationtrackignpoc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BgGreyDark,
    secondary = ColorPurple,
    tertiary = ColorSuccess,
    background = BgGreyDark,
    surface = BgWhiteDark,
    error = ColorDanger,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = TextWhite,
    outline = BorderDark,
    surfaceVariant = BgContentDark,
    surfaceTint = BgColorDark,
    surfaceContainer = BgWhiteDark,
    surfaceContainerHigh = BgWhiteDark,
    surfaceContainerHighest = BgWhiteDark
)

private val LightColorScheme = lightColorScheme(
    primary = BgWhiteDark,
    secondary = ColorPurple,
    tertiary = ColorSuccess,
    background = BgGreyLight,
    surface = BgWhiteLight,
    error = ColorDanger,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onError = TextWhite,
    outline = BorderLight,
    surfaceVariant = BgContentLight,
    surfaceTint = Primary,
    surfaceContainer = BgWhiteLight,
    surfaceContainerHigh = BgWhiteLight,
    surfaceContainerHighest = BgWhiteLight,
)

@Composable
fun LocationTrackignPOCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
