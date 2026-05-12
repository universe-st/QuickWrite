package com.universe_st.quickwriter.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 浅色模式色板 — 纸墨写作风 (Warm Literary / Ink & Paper)
// ============================================================

// Primary: 暖墨棕
val LightPrimary = Color(0xFF78716C)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE7E5E4)
val LightOnPrimaryContainer = Color(0xFF44403C)

// Secondary: 灰棕
val LightSecondary = Color(0xFFA8A29E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF5F5F4)
val LightOnSecondaryContainer = Color(0xFF57534E)

// Tertiary: 琥珀
val LightTertiary = Color(0xFFD97706)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF3C7)
val LightOnTertiaryContainer = Color(0xFF92400E)

// Surface / Background
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF5F0E8)
val LightOnSurfaceVariant = Color(0xFF44403C)
val LightBackground = Color(0xFFFFFBEB)
val LightOnBackground = Color(0xFF0F172A)

// Outline
val LightOutline = Color(0xFFD6D3D1)
val LightOutlineVariant = Color(0xFFE7E5E4)

// Error
val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF991B1B)

// Inverse
val LightInverseSurface = Color(0xFF1C1917)
val LightInverseOnSurface = Color(0xFFF5F0E8)
val LightInversePrimary = Color(0xFFA8A29E)

// ============================================================
// 暗色模式色板
// ============================================================

val DarkPrimary = Color(0xFFA8A29E)
val DarkOnPrimary = Color(0xFF44403C)
val DarkPrimaryContainer = Color(0xFF57534E)
val DarkOnPrimaryContainer = Color(0xFFE7E5E4)

val DarkSecondary = Color(0xFFC5BFBA)
val DarkOnSecondary = Color(0xFF44403C)
val DarkSecondaryContainer = Color(0xFF57534E)
val DarkOnSecondaryContainer = Color(0xFFF5F5F4)

val DarkTertiary = Color(0xFFF59E0B)
val DarkOnTertiary = Color(0xFF451A03)
val DarkTertiaryContainer = Color(0xFF92400E)
val DarkOnTertiaryContainer = Color(0xFFFEF3C7)

val DarkSurface = Color(0xFF282421)
val DarkOnSurface = Color(0xFFF5F0E8)
val DarkSurfaceVariant = Color(0xFF3E3A36)
val DarkOnSurfaceVariant = Color(0xFFD6D3D1)
val DarkBackground = Color(0xFF1C1917)
val DarkOnBackground = Color(0xFFF5F0E8)

val DarkOutline = Color(0xFF8D8986)
val DarkOutlineVariant = Color(0xFF3E3A36)

val DarkError = Color(0xFFFCA5A5)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFEE2E2)

val DarkInverseSurface = Color(0xFFF5F0E8)
val DarkInverseOnSurface = Color(0xFF1C1917)
val DarkInversePrimary = Color(0xFF78716C)

// ============================================================
// 向后兼容别名（过渡用，标记为 deprecated）
// ============================================================
@Deprecated("Use MaterialTheme.colorScheme.primary instead", ReplaceWith("MaterialTheme.colorScheme.primary"))
val PrimaryDark = LightPrimary

@Deprecated("Use MaterialTheme.colorScheme.secondary instead", ReplaceWith("MaterialTheme.colorScheme.secondary"))
val PrimaryLight = LightSecondary

@Deprecated("Use MaterialTheme.colorScheme.tertiary instead", ReplaceWith("MaterialTheme.colorScheme.tertiary"))
val Accent = LightTertiary

@Deprecated("Use MaterialTheme.colorScheme.onSurface instead", ReplaceWith("MaterialTheme.colorScheme.onSurface"))
val TextPrimary = LightOnSurface

@Deprecated("Use MaterialTheme.colorScheme.onSurfaceVariant instead", ReplaceWith("MaterialTheme.colorScheme.onSurfaceVariant"))
val TextSecondary = LightOnSurfaceVariant

@Deprecated("Use MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) instead", ReplaceWith("MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)"))
val TextDisabled = Color(0xFF9e9e9e)

@Deprecated("Use MaterialTheme.colorScheme.surface instead", ReplaceWith("MaterialTheme.colorScheme.surface"))
val BackgroundLight = Color(0xFFFAFAFA)

@Deprecated("Use MaterialTheme.colorScheme.inverseSurface instead", ReplaceWith("MaterialTheme.colorScheme.inverseSurface"))
val BackgroundDark = Color(0xFF121212)
