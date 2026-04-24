package com.universe_st.quickwriter.util

import android.graphics.Color
import com.universe_st.markor_editor.EditorConfig

class AppEditorConfig(
    private val isDark: Boolean,
    private val fontFamily: String = "",
    private val fontSizeSp: Int = 14
) : EditorConfig {

    override fun isDarkModeEnabled(): Boolean = isDark

    override fun getFontFamily(): String = fontFamily

    override fun getEditorForegroundColor(): Int =
        if (isDark) Color.WHITE else Color.BLACK
}
