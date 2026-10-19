package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

// Brand seed, mirrors app/src/main/res/values/colors.xml blue_normal.
val BrandSeed = Color(0xFF2D5EED)

// Dark-mode background identity, mirrors app/src/main/res/values-night/colors.xml background_color.
// Applied on BOTH the dynamic-color path and the static fallback path (see Theme.kt) so the
// brand's navy dark mode survives regardless of API level / dynamic color availability.
val NavyBackground = Color(0xFF1A1D29)

private val LightColorsBase: ColorScheme by lazy {
    dynamicColorScheme(seedColor = BrandSeed, isDark = false)
}

private val DarkColorsBase: ColorScheme by lazy {
    dynamicColorScheme(seedColor = BrandSeed, isDark = true).copy(
        background = NavyBackground,
        surfaceDim = NavyBackground,
    )
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Amber CRT identity (the app's default look). A monochrome amber-phosphor palette on warm near-
// black, evoking an old terminal. Built as fixed MD3 schemes (not seed-derived) so the whole app
// reads as one console regardless of device — the theme picker can still pin other seed colors,
// and enabling Dynamic Color in settings opts back into Material You.
// ─────────────────────────────────────────────────────────────────────────────────────────────

val AmberCrtDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB000),            // amber phosphor
    onPrimary = Color(0xFF140E00),
    primaryContainer = Color(0xFF3A2A00),
    onPrimaryContainer = Color(0xFFFFD480),
    secondary = Color(0xFFD9A441),
    onSecondary = Color(0xFF140E00),
    secondaryContainer = Color(0xFF4A3600),
    onSecondaryContainer = Color(0xFFFFDFA0),
    tertiary = Color(0xFF9FE88D),           // faint green-phosphor accent for "ok" states
    onTertiary = Color(0xFF0E1A00),
    tertiaryContainer = Color(0xFF2C3A10),
    onTertiaryContainer = Color(0xFFCFF3AE),
    background = Color(0xFF120D00),
    onBackground = Color(0xFFFFB000),
    surface = Color(0xFF140E00),
    onSurface = Color(0xFFFFB000),
    surfaceVariant = Color(0xFF2A1F00),
    onSurfaceVariant = Color(0xFFC8921A),    // dimmed amber for secondary text
    surfaceDim = Color(0xFF0D0900),
    surfaceContainerLowest = Color(0xFF0D0900),
    surfaceContainerLow = Color(0xFF160F00),
    surfaceContainer = Color(0xFF1C1400),
    surfaceContainerHigh = Color(0xFF241900),
    surfaceContainerHighest = Color(0xFF2E2100),
    outline = Color(0xFF7A5A00),
    outlineVariant = Color(0xFF4A3600),
    error = Color(0xFFFF6B4A),
    onError = Color(0xFF1A0600),
    errorContainer = Color(0xFF5A1A0A),
    onErrorContainer = Color(0xFFFFD2C4),
    inverseSurface = Color(0xFFFFB000),
    inverseOnSurface = Color(0xFF140E00),
    scrim = Color(0xFF000000),
)

val AmberCrtLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFF4DE),
    primaryContainer = Color(0xFFFFE0A8),
    onPrimaryContainer = Color(0xFF2A1A00),
    secondary = Color(0xFF7A5A1A),
    onSecondary = Color(0xFFFFF4DE),
    secondaryContainer = Color(0xFFF2DEB4),
    onSecondaryContainer = Color(0xFF2A1A00),
    tertiary = Color(0xFF3F6B2A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF2DD),
    onBackground = Color(0xFF2A1A00),
    surface = Color(0xFFFBF2DD),
    onSurface = Color(0xFF2A1A00),
    surfaceVariant = Color(0xFFEFE0C2),
    onSurfaceVariant = Color(0xFF6B5423),
    outline = Color(0xFFA98A4A),
    outlineVariant = Color(0xFFD8C290),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

/** App default + static fallback for API < 31: the amber-CRT identity. */
val LightColors: ColorScheme get() = AmberCrtLightColors

/** App default + static fallback for API < 31: the amber-CRT identity. */
val DarkColors: ColorScheme get() = AmberCrtDarkColors

/**
 * Semantic colors that don't map cleanly onto MD3's core roles (see plan §2) — kept outside
 * MaterialTheme.colorScheme so they never get silently reinterpreted by dynamic color.
 */
@Immutable
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val success: Color,
    val onSuccess: Color,
)

val LightExtendedColors = ExtendedColors(
    warning = Color(0xFF8A6A00),
    onWarning = Color(0xFFFFFFFF),
    success = Color(0xFF3F6B2A),
    onSuccess = Color(0xFFFFFFFF),
)

// Amber-CRT semantics: warning is the phosphor amber itself, success a faint green phosphor.
val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFFFB000),
    onWarning = Color(0xFF140E00),
    success = Color(0xFF9FE88D),
    onSuccess = Color(0xFF0E1A00),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
