# Writing Toolbar Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the save button in WritingTopBar with a "+" dropdown menu containing Save, Copy Full Text, and Copy Plain Text options.

**Architecture:** Modify the `WritingTopBar` composable to swap the save `IconButton` for an `Add` `IconButton` with a `DropdownMenu`. Copy logic accesses `state.editorContent` via `LocalClipboardManager`. A `SnackbarHost` is added to the `Scaffold` for copy feedback. Markdown stripping is a private function in WritingScreen.kt.

**Tech Stack:** Jetpack Compose, Material 3, Android ClipboardManager

---

### Task 1: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add English strings**

Insert after `writing_save_content_desc` (line 122) in `app/src/main/res/values/strings.xml`:

```xml
    <string name="writing_copy_full_text">Copy Full Text</string>
    <string name="writing_copy_plain_text">Copy Plain Text</string>
    <string name="writing_copied">Copied to clipboard</string>
```

- [ ] **Step 2: Add Simplified Chinese strings**

Insert after `writing_save_content_desc` (line 122) in `app/src/main/res/values-zh-rCN/strings.xml`:

```xml
    <string name="writing_copy_full_text">复制全文</string>
    <string name="writing_copy_plain_text">复制纯文本</string>
    <string name="writing_copied">已复制到剪贴板</string>
```

- [ ] **Step 3: Add Traditional Chinese strings**

Insert after `writing_save_content_desc` (line 122) in `app/src/main/res/values-zh-rTW/strings.xml`:

```xml
    <string name="writing_copy_full_text">複製全文</string>
    <string name="writing_copy_plain_text">複製純文字</string>
    <string name="writing_copied">已複製到剪貼板</string>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "feat: add copy-related string resources for writing toolbar menu"
```

---

### Task 2: Add SnackbarHost and clipboard/copy logic to WritingScreen

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt`
  - Add imports
  - Add `snackbarHostState`
  - Add `SnackbarHost` to Scaffold
  - Add `stripMarkdown` private function

- [ ] **Step 1: Add required imports to WritingScreen.kt**

Insert after line 33 (`import androidx.compose.ui.res.stringResource`):

```kotlin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
```

- [ ] **Step 2: Add snackbarHostState variable**

Insert after line 93 (`var showNewFolderDialog by remember { mutableStateOf(false) }`):

```kotlin
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
```

- [ ] **Step 3: Add SnackbarHost to Scaffold**

Modify the Scaffold block. Insert after line 121 (`contentWindowInsets = WindowInsets(0.dp)`):

```kotlin
        snackbarHost = { SnackbarHost(snackbarHostState) },
```

The Scaffold should now look like:

```kotlin
    Scaffold(
        topBar = {
            WritingTopBar(
                uiState = uiState,
                onBack = onNavigateToProjectList,
                onSave = {
                    val s = viewModel.uiState.value as? WritingUiState.Success ?: return@WritingTopBar
                    if (s.fileBrowserMode == FileBrowserMode.CHAPTERS) {
                        viewModel.saveCurrentChapter()
                    } else {
                        viewModel.saveCurrentFile()
                    }
                },
                showChapterList = showChapterList,
                onToggleChapterList = { showChapterList = !showChapterList },
                isChatTab = isChatTab,
                showChatSidebar = aiChatViewModel.showSidebar,
                onToggleChatSidebar = { aiChatViewModel.showSidebar = !aiChatViewModel.showSidebar }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WritingStatusBar(uiState)
        },
        contentWindowInsets = WindowInsets(0.dp)
    )
```

- [ ] **Step 4: Add stripMarkdown private function and copy helpers after the WritingScreen function (before the WritingTopBar function, around line 365)**

Insert after the closing `}` of `WritingScreen` function (before line 366 `@OptIn(ExperimentalMaterial3Api::class)`):

```kotlin

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("```[\\s\\S]*?```")) { "" }
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("__(.+?)__"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("^[>*+-]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^---+$", RegexOption.MULTILINE), "")
        .trim()
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt
git commit -m "feat: add SnackbarHost, clipboard support, and stripMarkdown to WritingScreen"
```

---

### Task 3: Modify WritingTopBar to replace save button with + menu

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt`
  - Change `WritingTopBar` signature: add `onCopyFullText` and `onCopyPlainText` callbacks
  - Replace save button with "+" button + DropdownMenu
  - Wire copy callbacks from WritingScreen

- [ ] **Step 1: Add DropdownMenu import**

Insert after line 23 (`import androidx.compose.material3.*` is already present, which covers `DropdownMenu` and `DropdownMenuItem`)

- [ ] **Step 2: Update WritingTopBar signature to accept copy callbacks**

Replace the existing function signature (lines 367-378):

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    uiState: WritingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit,
    isChatTab: Boolean = false,
    showChatSidebar: Boolean = false,
    onToggleChatSidebar: () -> Unit = {}
) {
```

With:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    uiState: WritingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit,
    isChatTab: Boolean = false,
    showChatSidebar: Boolean = false,
    onToggleChatSidebar: () -> Unit = {},
    onCopyFullText: (String) -> Unit = {},
    onCopyPlainText: (String) -> Unit = {}
) {
```

- [ ] **Step 3: Add menu expanded state**

Insert after line 387 (`val showSaveButton = ...`):

```kotlin
    var menuExpanded by remember { mutableStateOf(false) }
```

- [ ] **Step 4: Replace the save button with + button and DropdownMenu**

Replace lines 446-450 (the save button block):

```kotlin
                if (showSaveButton) {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.writing_save_content_desc))
                    }
                }
```

With:

```kotlin
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.writing_more_actions))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (showSaveButton) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.writing_save_content_desc)) },
                                    onClick = {
                                        menuExpanded = false
                                        onSave()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.writing_copy_full_text)) },
                                onClick = {
                                    menuExpanded = false
                                    val content = (uiState as? WritingUiState.Success)?.editorContent ?: ""
                                    onCopyFullText(content)
                                },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.writing_copy_plain_text)) },
                                onClick = {
                                    menuExpanded = false
                                    val content = (uiState as? WritingUiState.Success)?.editorContent ?: ""
                                    onCopyPlainText(content)
                                },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                            )
                        }
                    }
```

- [ ] **Step 5: Wire copy callbacks in WritingScreen's WritingTopBar call**

Modify the `WritingTopBar` call in the Scaffold (lines 99-116). Add the `onCopyFullText` and `onCopyPlainText` parameters:

The current call:
```kotlin
        topBar = {
            WritingTopBar(
                uiState = uiState,
                onBack = onNavigateToProjectList,
                onSave = {
                    val s = viewModel.uiState.value as? WritingUiState.Success ?: return@WritingTopBar
                    if (s.fileBrowserMode == FileBrowserMode.CHAPTERS) {
                        viewModel.saveCurrentChapter()
                    } else {
                        viewModel.saveCurrentFile()
                    }
                },
                showChapterList = showChapterList,
                onToggleChapterList = { showChapterList = !showChapterList },
                isChatTab = isChatTab,
                showChatSidebar = aiChatViewModel.showSidebar,
                onToggleChatSidebar = { aiChatViewModel.showSidebar = !aiChatViewModel.showSidebar }
            )
        },
```

Widen the `viewModelScope` / `coroutineScope` for snackbar launches by using `rememberCoroutineScope()`:

Insert after line 93 (alongside `snackbarHostState`):

```kotlin
    val coroutineScope = rememberCoroutineScope()
```

Then replace the `WritingTopBar` call with:

```kotlin
        topBar = {
            WritingTopBar(
                uiState = uiState,
                onBack = onNavigateToProjectList,
                onSave = {
                    val s = viewModel.uiState.value as? WritingUiState.Success ?: return@WritingTopBar
                    if (s.fileBrowserMode == FileBrowserMode.CHAPTERS) {
                        viewModel.saveCurrentChapter()
                    } else {
                        viewModel.saveCurrentFile()
                    }
                },
                showChapterList = showChapterList,
                onToggleChapterList = { showChapterList = !showChapterList },
                isChatTab = isChatTab,
                showChatSidebar = aiChatViewModel.showSidebar,
                onToggleChatSidebar = { aiChatViewModel.showSidebar = !aiChatViewModel.showSidebar },
                onCopyFullText = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.writing_copied)
                        )
                    }
                },
                onCopyPlainText = { text ->
                    clipboardManager.setText(AnnotatedString(stripMarkdown(text)))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.writing_copied)
                        )
                    }
                }
            )
        },
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt
git commit -m "feat: replace save button with + dropdown menu in WritingTopBar"
```

---

### Task 4: Add missing string resource for + button tooltip

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add `writing_more_actions` string**

Insert into all three strings.xml files alongside the copy strings from Task 1:

English (`values/strings.xml`):
```xml
    <string name="writing_more_actions">More Actions</string>
```

Simplified Chinese (`values-zh-rCN/strings.xml`):
```xml
    <string name="writing_more_actions">更多操作</string>
```

Traditional Chinese (`values-zh-rTW/strings.xml`):
```xml
    <string name="writing_more_actions">更多操作</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "feat: add writing_more_actions string resource"
```

---

### Task 5: Add missing imports and verify build

- [ ] **Step 1: Ensure `kotlinx.coroutines.launch` import**

In `WritingScreen.kt`, the import for `launch` should be present. Check that line 64 (`import android.widget.Toast`) is not the last import - add:

```kotlin
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Remove unused `Icons.Default.Save` import from WritingScreen.kt if no other usage**

`Icons.Default.Save` is used in the new menu item inside WritingTopBar, so keep it.

- [ ] **Step 3: Build and verify**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit final changes**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt
git commit -m "chore: add missing coroutines launch import for WritingScreen"
```
