package edu.nd.pmcburne.hwapp.one.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WarmOrange,
    secondary = SoftOrange,
    background = CreamBackground,
    surface = SoftCream,
    onPrimary = BrownText,
    onSecondary = BrownText,
    onBackground = BrownText,
    onSurface = BrownText
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE6A85E),
    secondary = Color(0xFFD8B07A),
    background = Color(0xFF1E1A17),
    surface = Color(0xFF2A2521),
    onPrimary = Color(0xFFF8EEDF),
    onSecondary = Color(0xFFF8EEDF),
    onBackground = Color(0xFFF8EEDF),
    onSurface = Color(0xFFF8EEDF)
)

@Composable
fun HWStarterRepoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}