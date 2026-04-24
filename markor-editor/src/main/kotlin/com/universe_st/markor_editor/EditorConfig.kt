package com.universe_st.markor_editor

import android.graphics.Color
import androidx.annotation.ColorInt

interface EditorConfig {
    fun isSpellingRedUnderlineEnabled(): Boolean = false
    fun isDarkModeEnabled(): Boolean = false
    fun getFontFamily(): String = ""
    @ColorInt fun getEditorForegroundColor(): Int = Color.BLACK
    fun getTabWidth(): Int = 4
    fun isDebugEnabled(): Boolean = false
}
