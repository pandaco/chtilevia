package fr.chtilevia.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Couleurs de l'icône : le rouge pour les actions, le blanc (ou le noir) pour le fond, des gris
 * neutres pour le reste. Tous les rôles Material sont fixés, sinon il en déduit des roses pâles.
 */
object Brand {
    val red = Color(0xFFE3051B)
    val redPressed = Color(0xFFB00415)
    val ink = Color(0xFF1B1B1B)
    val muted = Color(0xFF6B6B6B)
    val line = Color(0xFFE6E6E6)
    val field = Color(0xFFF4F4F4)
    val night = Color(0xFF121212)
    val nightField = Color(0xFF242424)
    val nightLine = Color(0xFF333333)
    val nightMuted = Color(0xFFA6A6A6)

    /** Passage en temps réel : le vert « en direct » des applis de transport, bien distinct du rouge */
    val live = Color(0xFF1E8E3E)
}

private val Light = lightColorScheme(
    primary = Brand.red,
    onPrimary = Color.White,
    primaryContainer = Brand.red,
    onPrimaryContainer = Color.White,
    secondary = Brand.ink,
    onSecondary = Color.White,
    secondaryContainer = Brand.field,
    onSecondaryContainer = Brand.ink,
    tertiary = Brand.red,
    background = Color.White,
    onBackground = Brand.ink,
    surface = Color.White,
    onSurface = Brand.ink,
    surfaceVariant = Brand.field,
    onSurfaceVariant = Brand.muted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Brand.field,
    surfaceContainer = Brand.field,
    surfaceContainerHigh = Brand.field,
    surfaceContainerHighest = Brand.field,
    outline = Brand.line,
    outlineVariant = Brand.line,
)

private val Dark = darkColorScheme(
    primary = Brand.red,
    onPrimary = Color.White,
    primaryContainer = Brand.red,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Brand.night,
    secondaryContainer = Brand.nightField,
    onSecondaryContainer = Color.White,
    tertiary = Brand.red,
    background = Brand.night,
    onBackground = Color.White,
    surface = Brand.night,
    onSurface = Color.White,
    surfaceVariant = Brand.nightField,
    onSurfaceVariant = Brand.nightMuted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Brand.night,
    surfaceContainerLow = Brand.nightField,
    surfaceContainer = Brand.nightField,
    surfaceContainerHigh = Brand.nightField,
    surfaceContainerHighest = Brand.nightField,
    outline = Brand.nightLine,
    outlineVariant = Brand.nightLine,
)

@Composable
fun ChtiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
