package com.universe_st.quickwriter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// For now, use system default fonts. Custom fonts will be added when res/font/ files are available.
private val CormorantGaramondFamily = FontFamily.Default
private val LibreBaskervilleFamily = FontFamily.Default
private val InterFamily = FontFamily.Default
private val JetBrainsMonoFamily = FontFamily.Default

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Medium
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Medium
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    )
)
