package com.universe_st.markor_editor

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter
import net.gsantner.markor.frontend.textview.HighlightingEditor
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlin.math.min

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
    initialScrollY: Int = 0,
    initialSelectionStart: Int = 0,
    onDispose: ((scrollY: Int, selectionStart: Int) -> Unit)? = null,
    onAddToConversation: ((selectedText: String, startLine: Int, endLine: Int) -> Unit)? = null,
    addToConversationLabel: String = "Add to Chat"
) {
    var editorView by remember { mutableStateOf<HighlightingEditor?>(null) }

    var currentOnAddToConversation by remember {
        mutableStateOf(onAddToConversation)
    }
    var currentAddToConversationLabel by remember {
        mutableStateOf(addToConversationLabel)
    }

    DisposableEffect(Unit) {
        onDispose {
            editorView?.let { view ->
                onDispose?.invoke(view.scrollY, view.selectionStart)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            HighlightingEditor(context, null).apply {
                gravity = Gravity.TOP or Gravity.START
                isFocusable = true
                isFocusableInTouchMode = true
                HighlightingEditor.setDefaultConfig(editorConfig)
                setTextColor(editorConfig.getEditorForegroundColor())
                setHighlighter(createHighlighter(editorConfig, highlightingMode))
                setHighlightingEnabled(true)

                if (initialScrollY > 0 || initialSelectionStart > 0) {
                    post {
                        scrollY = initialScrollY
                        if (initialSelectionStart >= 0) {
                            setSelection(initialSelectionStart.coerceIn(0, length()))
                        }
                    }
                }

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

                if (onAddToConversation != null) {
                    setCustomSelectionActionModeCallback(object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                            menu?.add(0, 0, 0, "☰")
                            menu?.add(0, 1, 1, currentAddToConversationLabel)
                            return true
                        }

                        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                            when (item?.itemId) {
                                0 -> this@apply.selectLines()
                                1 -> {
                                        val selStart = min(selectionStart, selectionEnd)
                                        val selEnd = kotlin.math.max(selectionStart, selectionEnd)
                                        if (selStart != selEnd) {
                                            val fullText = text?.toString() ?: return false
                                            val selectedText = fullText.substring(selStart, selEnd)
                                            val startLine = fullText.substring(0, selStart).count { it == '\n' }
                                            var endLine = fullText.substring(0, selEnd).count { it == '\n' }
                                            if (selectedText.endsWith("\n")) {
                                                endLine = (endLine - 1).coerceAtLeast(startLine)
                                            }
                                            currentOnAddToConversation?.invoke(selectedText, startLine, endLine)
                                        mode?.finish()
                                    }
                                }
                            }
                            return true
                        }

                        override fun onDestroyActionMode(mode: ActionMode?) {}
                        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
                    })
                }
            }
        },
        update = { view ->
            editorView = view
            currentOnAddToConversation = onAddToConversation
            currentAddToConversationLabel = addToConversationLabel
            view.isEnabled = enabled
            view.setTextColor(editorConfig.getEditorForegroundColor())

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
                view.setHighlighter(createHighlighter(editorConfig, highlightingMode))
                view.initHighlighter()
                view.setHighlightingEnabled(true)
                view.postInvalidate()
            }
        }
    )
}

private fun createHighlighter(config: EditorConfig, mode: HighlightingMode) = when (mode) {
    HighlightingMode.PLAINTEXT -> PlaintextSyntaxHighlighter(config)
    HighlightingMode.MARKDOWN -> MarkdownSyntaxHighlighter(config)
}

private class DefaultEditorConfig : EditorConfig
