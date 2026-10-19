package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

// Monospace "terminal" type scale — the single biggest lever for the amber-CRT / hacker identity.
// Uses the platform monospace family (no bundled font asset), so it works on every device and adds
// no APK weight. To swap in a dedicated terminal face later, drop a .ttf into res/font and replace
// `Mono` below with a FontFamily built from it (e.g. JetBrains Mono / Share Tech Mono).
private val Mono = FontFamily.Monospace

private val Base = Typography()

val SpecterTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Mono),
    displayMedium = Base.displayMedium.copy(fontFamily = Mono),
    displaySmall = Base.displaySmall.copy(fontFamily = Mono),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Mono),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Mono),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Mono),
    titleLarge = Base.titleLarge.copy(fontFamily = Mono),
    titleMedium = Base.titleMedium.copy(fontFamily = Mono),
    titleSmall = Base.titleSmall.copy(fontFamily = Mono),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Mono),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Mono),
    bodySmall = Base.bodySmall.copy(fontFamily = Mono),
    labelLarge = Base.labelLarge.copy(fontFamily = Mono),
    labelMedium = Base.labelMedium.copy(fontFamily = Mono),
    labelSmall = Base.labelSmall.copy(fontFamily = Mono),
)
