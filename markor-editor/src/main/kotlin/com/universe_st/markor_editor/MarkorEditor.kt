package com.universe_st.markor_editor

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter
import net.gsantner.markor.frontend.textview.HighlightingEditor

enum class HighlightingMode {
    PLAINTEXT,
    MARKDOWN
}

@Composable
fun MarkorEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editorConfig: EditorConfig = DefaultEditorConfig(),
    highlightingMode: HighlightingMode = HighlightingMode.PLAINTEXT,
    enabled: Boolean = true,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            HighlightingEditor(context, null).apply {
                gravity = Gravity.TOP or Gravity.START
                isFocusable = true
                isFocusableInTouchMode = true
                HighlightingEditor.setDefaultConfig(editorConfig)
                setHighlighter(createHighlighter(highlightingMode))
                setHighlightingEnabled(true)

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val newText = s?.toString() ?: ""
                        if (newText != value) {
                            onValueChange(newText)
                        }
                    }
                })
            }
        },
        update = { view ->
            view.isEnabled = enabled

            if (!view.text.contentEquals(value)) {
                val savedScrollY = view.scrollY
                val savedSelStart = view.selectionStart
                view.text?.replace(0, view.length(), value)
                val newLen = view.length()
                val targetSel = if (view.length() == value.length) {
                    savedSelStart.coerceIn(0, newLen)
                } else {
                    0
                }
                view.setSelection(targetSel)
                view.scrollY = savedScrollY
            }

            val currentType = view.highlighter?.javaClass?.kotlin
            val targetType = when (highlightingMode) {
                HighlightingMode.PLAINTEXT -> PlaintextSyntaxHighlighter::class
                HighlightingMode.MARKDOWN -> MarkdownSyntaxHighlighter::class
            }
            if (currentType != targetType) {
                view.setHighlighter(createHighlighter(highlightingMode))
                view.initHighlighter()
                view.setHighlightingEnabled(true)
                view.postInvalidate()
            }
        }
    )
}

private fun createHighlighter(mode: HighlightingMode) = when (mode) {
    HighlightingMode.PLAINTEXT -> PlaintextSyntaxHighlighter()
    HighlightingMode.MARKDOWN -> MarkdownSyntaxHighlighter()
}

private class DefaultEditorConfig : EditorConfig
