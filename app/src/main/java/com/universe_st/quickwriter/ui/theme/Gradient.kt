package com.universe_st.quickwriter.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val PrimaryGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1a237e), Color(0xFF283593)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)

val PrimaryGradientHorizontal = Brush.horizontalGradient(
    colors = listOf(Color(0xFF1a237e), Color(0xFF283593))
)

val SplashGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF121858), Color(0xFF1a237e), Color(0xFF283593)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)
