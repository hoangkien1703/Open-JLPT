package com.openjlpt.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Crimson = Color(0xFFC8102E)
private val Indigo = Color(0xFF1F2A44)

val CorrectGreen = Color(0xFF2E7D32)
val CorrectGreenContainer = Color(0xFFDDF3DF)
val WrongRed = Color(0xFFC62828)
val WrongRedContainer = Color(0xFFFBE0E0)

private val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF131C2F),
    tertiary = Color(0xFF6B5E10),
    tertiaryContainer = Color(0xFFF6E388),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
    surfaceVariant = Color(0xFFF3ECE4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AF),
    onPrimary = Color(0xFF68000F),
    primaryContainer = Color(0xFF93001C),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF283145),
    secondaryContainer = Color(0xFF3F475C),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFFD9C76F),
    tertiaryContainer = Color(0xFF524600),
)

@Composable
fun OpenJlptTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
