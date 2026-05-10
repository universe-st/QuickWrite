# "添加到对话"功能 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在编辑器选中文本后，通过选择菜单"添加到对话"将引用块添加到 Chat Tab 输入区上方，发送时拼接引用行号信息。

**Architecture:** WritingViewModel 管理 ReferenceBlock 列表（StateFlow）。MarkorEditor 层覆盖 ActionMode.Callback 新增菜单项，通过回调传递选中文本和行号。ChatTab 接收引用块列表并渲染，发送消息时在 WritingScreen 中合并引用信息后调用 AiChatViewModel 发送。

**Tech Stack:** Kotlin, Jetpack Compose, Android ActionMode

---

### 文件结构

| 文件 | 职责 |
|------|------|
| `markor-editor/.../MarkorEditor.kt` | 新增 `onAddToConversation` + `addToConversationLabel` 参数，覆盖 ActionMode.Callback |
| `app/.../viewmodel/WritingViewModel.kt` | ReferenceBlock 数据类 + referenceBlocks 状态 + add/remove/clear 方法 |
| `app/.../viewmodel/AiChatViewModel.kt` | 新增 `sendMessageWithContent()` 方法 |
| `app/.../screens/WritingScreen.kt` | 连接编辑器回调 → ViewModel，传递引用块到 ChatTab |
| `app/.../screens/ChatTab.kt` | 新增参数 + ReferenceBlockBar 组件 + onSend 合并逻辑 |
| `app/src/main/assets/prompts/novel_writing_assistant.md` | 新增引用格式规则 |
| `app/src/main/res/values/strings.xml` | 6 条英文字符串 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 6 条简体中文字符串 |
| `app/src/main/res/values-zh-rTW/strings.xml` | 6 条繁体中文字符串 |

---

### Task 1: 字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: 在三个语言文件中添加 6 条新字符串**

在 `values/strings.xml` 末尾 `</resources>` 前添加：

```xml
    <!-- ===== Reference Blocks ===== -->
    <string name="chat_add_to_conversation">Add to Chat</string>
    <string name="chat_reference_label">Reference</string>
    <string name="chat_reference_max_reached">Maximum 5 references</string>
    <string name="chat_reference_remove">Remove reference</string>
    <string name="chat_no_session_selected">Please select a conversation first</string>
    <string name="chat_reference_added">Reference added</string>
```

在 `values-zh-rCN/strings.xml` 末尾 `</resources>` 前添加：

```xml
    <!-- ===== 引用块 ===== -->
    <string name="chat_add_to_conversation">添加到对话</string>
    <string name="chat_reference_label">引用</string>
    <string name="chat_reference_max_reached">最多添加5个引用</string>
    <string name="chat_reference_remove">移除引用</string>
    <string name="chat_no_session_selected">请先选择一个对话</string>
    <string name="chat_reference_added">已添加引用</string>
```

在 `values-zh-rTW/strings.xml` 末尾 `</resources>` 前添加：

```xml
    <!-- ===== 引用區塊 ===== -->
    <string name="chat_add_to_conversation">新增至對話</string>
    <string name="chat_reference_label">引用</string>
    <string name="chat_reference_max_reached">最多新增5個引用</string>
    <string name="chat_reference_remove">移除引用</string>
    <string name="chat_no_session_selected">請先選擇一個對話</string>
    <string name="chat_reference_added">已新增引用</string>
```

---

### Task 2: AiChatViewModel — 新增 sendMessageWithContent 方法

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/AiChatViewModel.kt`

- [ ] **Step 1: 在 `AiChatViewModel` 类中添加新方法**

在 `sendMessage()` 方法后面（第263行之后）添加：

```kotlin
    fun sendMessageWithContent(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        if (!hasModelConfig) return
        val service = chatService ?: return
        val sessionId = currentSessionId ?: return

        try {
            service.sendMessage(sessionId, trimmed)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.sendMessageWithContent failed") }
    }
```

---

### Task 3: WritingViewModel — 添加 ReferenceBlock 和引用管理

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/WritingViewModel.kt`

- [ ] **Step 1: 新增 ReferenceBlock 数据类**

在 `WritingViewModel.kt` 文件顶部（`WritingUiState` 前，import 区域后）添加：

```kotlin
data class ReferenceBlock(
    val id: String,
    val filePath: String,
    val contentPreview: String,
    val startLine: Int,
    val endLine: Int
)
```

- [ ] **Step 2: 在 WritingUiState.Success 中添加 referenceBlocks 字段**

修改 `WritingUiState.Success` 数据类，在 `editorSelectionStart: Int = 0` 之后添加：

```kotlin
        val referenceBlocks: List<ReferenceBlock> = emptyList()
```

- [ ] **Step 3: 在 WritingViewModel 类中添加引用管理方法**

在 `saveEditorScrollPosition` 方法后（第743行之后）、`refreshNonChapterFile` 前添加以下方法：

```kotlin
    fun addReference(filePath: String, selectedText: String, bodyStartLine: Int, bodyEndLine: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val blocks = state.referenceBlocks
        if (blocks.size >= 5) return

        val fullContent = projectManagementUseCase.readFileContent(filePath).getOrDefault("")
        val fmOffset = calculateFrontMatterLineCount(fullContent)
        val fileStartLine = bodyStartLine + fmOffset + 1
        val fileEndLine = bodyEndLine + fmOffset + 1

        val lines = selectedText.lines()
        val preview = if (lines.size > 2) {
            lines.take(2).joinToString("\n") + "..."
        } else {
            lines.joinToString("\n")
        }

        val block = ReferenceBlock(
            id = java.util.UUID.randomUUID().toString(),
            filePath = filePath.removePrefix(state.project.storagePath + "/"),
            contentPreview = preview,
            startLine = fileStartLine,
            endLine = fileEndLine
        )
        _uiState.value = state.copy(referenceBlocks = blocks + block)
        setSelectedTab(1)
    }

    fun removeReference(id: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        _uiState.value = state.copy(
            referenceBlocks = state.referenceBlocks.filter { it.id != id }
        )
    }

    fun clearReferences() {
        val state = _uiState.value as? WritingUiState.Success ?: return
        _uiState.value = state.copy(referenceBlocks = emptyList())
    }

    fun buildMessageWithReferences(userInput: String, references: List<ReferenceBlock>): String {
        if (references.isEmpty()) return userInput
        val refLines = references.map { ref ->
            if (ref.startLine == ref.endLine)
                "[引用 ${ref.filePath}:${ref.startLine}]"
            else
                "[引用 ${ref.filePath} ${ref.startLine}-${ref.endLine}]"
        }
        return refLines.joinToString("\n") + "\n" + userInput
    }

    private fun calculateFrontMatterLineCount(fullContent: String): Int {
        val lines = fullContent.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") return 0
        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (endIndex == -1) return 0
        return endIndex + 2
    }
```

- [ ] **Step 4: 修改 `setSelectedTab` 方法 — 确保引用块保留在切换 tab 时不被丢失**

在 `setSelectedTab` 方法中的 state copy 处，保留现有 referenceBlocks。当前代码：
```kotlin
if (tab == success.selectedTab) return
_uiState.value = success.copy(selectedTab = tab)
```
无需修改 — `copy` 默认保留所有字段。

检查 `loadChapters` 中的 emitEmptyState=true 分支（第261行）和 emitEmptyState=false 分支（第304行），确保 referenceBlocks 被保留：

在第262行的 `WritingUiState.Success(...)` constructor 调用后添加一行，改为保存当前的 referenceBlocks：
但更简单的方式是在 emitEmptyState=true 的 state 构建后立即设置引用块。实际上，由于 `loadChapters` 在多个地方调用（切换章节、重载），每次都会重建 Success state，这会丢失 referenceBlocks。需要保留现有引用块。

修改 `loadChapters` 方法中的两处 `WritingUiState.Success(...)` 构造：

**第262行处**，在现有 constructor 末尾追加：
```kotlin
                referenceBlocks = (prevSuccess as? WritingUiState.Success?)?.referenceBlocks ?: emptyList()
```

**第304行处**，在现有 constructor 末尾追加：
```kotlin
                referenceBlocks = prevSuccess?.referenceBlocks ?: emptyList()
```

同时补全 prevSuccess 引用（第182行的定义已存在，直接使用）。

---

### Task 4: MarkorEditor — 添加选择菜单回调

**Files:**
- Modify: `markor-editor/src/main/kotlin/com/universe_st/markor_editor/MarkorEditor.kt`

- [ ] **Step 1: 新增参数**

修改 `MarkorEditor` 函数签名，在现有参数列表末尾（`onDispose` 之后）添加两个新参数：

```kotlin
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
```

- [ ] **Step 2: 在 factory lambda 中覆盖 ActionMode.Callback**

在 `factory = { context -> ... }` 的 `apply` 块中，`addTextChangedListener` 之后（第75行之后）添加：

```kotlin
                if (onAddToConversation != null) {
                    setCustomSelectionActionModeCallback(object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                            menu?.add(0, 0, 0, "☰")
                            menu?.add(0, 1, 1, addToConversationLabel)
                            return true
                        }

                        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                            when (item?.itemId) {
                                0 -> this@apply.selectLines()
                                1 -> {
                                    val selStart = minOf(selectionStart, selectionEnd)
                                    val selEnd = maxOf(selectionStart, selectionEnd)
                                    if (selStart != selEnd) {
                                        val text = text.substring(selStart, selEnd)
                                        val layout = layout ?: return false
                                        val startLine = layout.getLineForOffset(selStart)
                                        val endLine = layout.getLineForOffset(selEnd)
                                        onAddToConversation.invoke(text.toString(), startLine, endLine)
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
```

- [ ] **Step 3: 添加缺失的 import**

在 import 区域添加：

```kotlin
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlin.math.min
```

---

### Task 5: ChatTab — 添加 ReferenceBlockBar 和引用合并逻辑

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ChatTab.kt`

- [ ] **Step 1: 修改 ChatTab 函数签名 — 新增参数**

修改 `ChatTab` 函数签名：

```kotlin
@Composable
fun ChatTab(
    viewModel: AiChatViewModel,
    projectId: String,
    onNavigateToAiConfig: () -> Unit = {},
    isNoProjectMode: Boolean = false,
    onNavigateToProjectList: () -> Unit = {},
    referenceBlocks: List<com.universe_st.quickwriter.presentation.viewmodel.ReferenceBlock> = emptyList(),
    onRemoveReference: (String) -> Unit = {},
    onReferencesCleared: () -> Unit = {}
) {
```

- [ ] **Step 2: 修改 ChatContentArea 的调用 — 合并引用块逻辑**

在 ChatTab 中，找到 `ChatContentArea(...)` 调用处（大约第137行）。修改 `onSend` 参数：

替换：
```kotlin
                    onSend = { viewModel.sendMessage() },
```

为：
```kotlin
                    onSend = {
                        val blocks = referenceBlocks
                        if (blocks.isNotEmpty()) {
                            val refLines = blocks.joinToString("\n") { ref ->
                                if (ref.startLine == ref.endLine)
                                    "[引用 ${ref.filePath}:${ref.startLine}]"
                                else
                                    "[引用 ${ref.filePath} ${ref.startLine}-${ref.endLine}]"
                            }
                            val input = viewModel.inputText.trim()
                            if (input.isNotEmpty()) {
                                viewModel.sendMessageWithContent("$refLines\n$input")
                                onReferencesCleared()
                            }
                        } else {
                            viewModel.sendMessage()
                        }
                    },
```

同时在 ChatContentArea 调用中添加 `referenceBlocks` 和 `onRemoveReference` 参数。ChatContentArea 函数签名和调用需同步修改。

- [ ] **Step 3: 修改 ChatContentArea 函数签名 — 新增引用参数**

```kotlin
@Composable
private fun ChatContentArea(
    currentSessionId: String?,
    messages: List<ChatMessage>,
    sessionState: SessionState,
    inputText: String,
    isGenerating: Boolean,
    partialContent: String?,
    streamingReasoning: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDeleteMessage: (Int) -> Unit,
    referenceBlocks: List<com.universe_st.quickwriter.presentation.viewmodel.ReferenceBlock> = emptyList(),
    onRemoveReference: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
```

- [ ] **Step 4: 在 ChatContentArea 中添加 ReferenceBlockBar**

在 `ChatContentArea` 的 `Box(modifier = modifier)` 中，`Column` 内，`ChatInputArea` 之前（大约第757行前）添加：

```kotlin
            if (referenceBlocks.isNotEmpty()) {
                ReferenceBlockBar(
                    blocks = referenceBlocks,
                    onRemove = onRemoveReference,
                    modifier = Modifier.fillMaxWidth()
                )
            }
```

- [ ] **Step 5: 添加 ReferenceBlockBar Composable 组件**

在 `ChatTab.kt` 文件末尾（`ChatInputArea` 函数之后、文件结束前）添加新组件：

```kotlin
@Composable
private fun ReferenceBlockBar(
    blocks: List<com.universe_st.quickwriter.presentation.viewmodel.ReferenceBlock>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(R.string.chat_reference_label)} ${block.filePath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = block.contentPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemove(block.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_reference_remove),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

---

### Task 6: WritingScreen — 连接编辑器回调和 ChatTab

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt`

- [ ] **Step 1: 在 EditorContent 调用中传递 onAddToConversation**

在 `WritingScreen` 函数的 `EditorContent(...)` 调用处（大约第172行），`onEditorDispose` 参数之后添加新参数。修改 `MarkorEditor` 调用（第585行），在现有参数后追加：

```kotlin
                            onAddToConversation = { selectedText, startLine, endLine ->
                                val currentState = viewModel.uiState.value as? WritingUiState.Success ?: return@MarkorEditor
                                if (aiChatViewModel.currentSessionId == null) {
                                    // Show toast - use Android Toast for simplicity
                                    android.widget.Toast.makeText(
                                        LocalContext.current,
                                        LocalContext.current.getString(R.string.chat_no_session_selected),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@MarkorEditor
                                }
                                val filePath = when {
                                    currentState.fileBrowserMode == FileBrowserMode.CHAPTERS && currentState.currentChapterIndex >= 0 ->
                                        currentState.chapters[currentState.currentChapterIndex].filePath
                                    currentState.currentFilePath != null ->
                                        currentState.currentFilePath
                                    else -> return@MarkorEditor
                                }
                                viewModel.addReference(filePath, selectedText, startLine, endLine)
                            },
                            addToConversationLabel = stringResource(R.string.chat_add_to_conversation)
```

注意：`MarkorEditor` 被包裹在 `EditorContent` 中。需要透传 `onAddToConversation` 和 `addToConversationLabel` 参数。

因此需要在 `EditorContent` 函数签名（第471行）中添加：

```kotlin
    onAddToConversation: ((String, Int, Int) -> Unit)? = null,
    addToConversationLabel: String = "Add to Chat",
```

并在 MarkorEditor 调用处（第585行）使用这些参数。

- [ ] **Step 2: 修改 ChatTab 调用 — 传递引用参数**

在 WritingScreen 中，有两处 ChatTab 调用：

**第一处（NoProject 模式，第137行）**：添加默认空参数：

```kotlin
                    ChatTab(
                        viewModel = aiChatViewModel,
                        projectId = SessionManager.NO_PROJECT_ID,
                        onNavigateToAiConfig = onNavigateToAiConfig,
                        isNoProjectMode = true,
                        onNavigateToProjectList = onNavigateToProjectList,
                        referenceBlocks = emptyList()
                    )
```

**第二处（Success 模式，第202行）**：传递引用块并处理回调：

```kotlin
                            ChatTab(
                                viewModel = aiChatViewModel,
                                projectId = state.project.id,
                                onNavigateToAiConfig = onNavigateToAiConfig,
                                referenceBlocks = state.referenceBlocks,
                                onRemoveReference = { id -> viewModel.removeReference(id) },
                                onReferencesCleared = { viewModel.clearReferences() }
                            )
```

- [ ] **Step 3: 切换项目时清空引用块**

在 `WritingViewModel.setSelectedTab` 方法中，项目切换逻辑已由 `loadChapters` 处理（Task 3 Step 4 已确保保留引用块）。需要在 `loadCurrentProject` 方法（第98行）中清空引用块。每次重新加载项目时（项目发生变化），引用块应被清空。

修改 `loadCurrentProject` 中第100行的 `WritingUiState.Loading` 状态设置处：实际上重载会自动通过 `loadChapters` 重建，而 `loadChapters` 中通过 `prevSuccess` 获取之前的引用块。若 prevSuccess 为 null（首次加载），则引用块为空列表。这是正确的行为。

检查是否需要特殊处理项目切换清空：如果用户在一个项目中添加了引用块，然后切换到另一个项目，引用块应清空。当前 `loadChapters` 在 `loadCurrentProject` 调用时 prevSuccess 可能是旧项目的，但由于 projectId 比较（`project.id == lastProjectId`）在 loadChapters 中已处理，如果项目不同则不会设置 selectedTab。引用块需要在项目切换时清空。

最简单的方式：在 `loadCurrentProject` 中清空引用块是通过 loadChapters 的 prevSuccess 处理。由于 `_uiState.value` 被设为 Loading（第100行），然后重建为 Success，prevSuccess 会获取旧的 Loading 状态而非旧项目的 Success 状态，因此引用块自然为空。

实际上，更明确的做法是：不需要额外处理。当 `loadCurrentProject` 被调用时（即重新进入写作），所有状态被重建，引用块自然为空列表（默认值 `emptyList()`）。

---

### Task 7: 系统提示词更新

**Files:**
- Modify: `app/src/main/assets/prompts/novel_writing_assistant.md`

- [ ] **Step 1: 在提示词中新增引用格式规则**

在文件的"可用能力"章节之后（第91行后，即 `## 工作流程建议` 之前）添加：

```markdown
## 引用格式规则

用户消息中可能出现 `[引用 文件路径 行号]` 格式的引用行，表示用户选中了某段内容作为提问上下文。

格式：
- 单行引用：`[引用 正文/第一章.md:12]` 表示引用该文件第12行
- 多行引用：`[引用 正文/第一章.md 12-15]` 表示引用该文件第12行至第15行

规则：
1. 引用行不是用户的自然语言，它标记了用户引用的上下文来源。
2. 行号是文件中的真实行号（包含 YAML Front Matter 首部），与 ViewFile 工具显示的行号完全一致。
3. 用户的话语（引用行之后的内容）是基于引用内容的提问或指令。
4. 你应结合引用内容和用户话语来理解和回应，必要时使用 ViewFile 再次确认上下文。

示例：
用户消息：
[引用 正文/第一章.md 12-15]
这段开头是不是太突兀了？

理解：用户引用了第一章第12-15行，认为开头太突兀，希望获得改进建议。
动作：使用 ViewFile 查看 正文/第一章.md，定位到12-15行，结合上下文给出修改建议。
```

---

### Task 8: 构建验证

**Files:** 无

- [ ] **Step 1: 运行 Gradle 构建验证编译通过**

```bash
./gradlew :app:assembleDebug
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2: 手动测试清单**

1. 启动应用 → 进入写作 → 编辑器 Tab → 选中一段文本 → 验证选择菜单出现"添加到对话"
2. 点击"添加到对话"（无选中会话时）→ 验证 Toast"请先选择一个对话"
3. 切换到 Chat Tab → 创建一个会话 → 回到编辑器 → 选中文本 → 点击"添加到对话" → 验证自动切换到 Chat Tab → 验证引用块出现在输入框上方
4. 验证引用块显示：文件路径、内容前两行、省略号、×按钮
5. 点击 × → 验证引用块被移除
6. 反复添加引用块至第6个 → 验证 Toast"最多添加5个引用"
7. 输入消息 → 点击发送 → 验证消息内容包含引用行 → 验证引用块清空
8. 切换章节 → 验证引用块保留
9. 切换项目 → 验证引用块清空
10. 验证 AI 能理解引用内容（通过提示词更新）

---

## 自查清单

**Spec coverage:**
- [x] 数据模型 (ReferenceBlock) — Task 3
- [x] 状态存放 (WritingUiState.Success) — Task 3
- [x] 编辑器选择菜单 — Task 4
- [x] 前端 Matter 行号偏移 — Task 3 (calculateFrontMatterLineCount)
- [x] 引用块 UI — Task 5
- [x] 消息拼接 — Task 5
- [x] 系统提示词 — Task 7
- [x] 字符串资源 — Task 1
- [x] 会话检查 + Toast — Task 6
- [x] 数量上限 5 个 — Task 3
- [x] 发送后清空 — Task 5
- [x] 切换章节保留 — Task 3 Step 4
- [x] 切换项目清空 — Task 6 Step 3

**Type consistency:**
- ReferenceBlock 定义在 WritingViewModel.kt (Task 3)，在 ChatTab.kt (Task 5) 和 WritingScreen.kt (Task 6) 中引用
- `sendMessageWithContent` 在 AiChatViewModel.kt (Task 2)，在 ChatTab.kt (Task 5) 中调用
- `addReference` / `removeReference` / `clearReferences` 在 WritingViewModel (Task 3)，在 WritingScreen (Task 6) 中调用
- `onAddToConversation` 回调类型为 `(String, Int, Int) -> Unit`，MarkorEditor (Task 4) 和 WritingScreen (Task 6) 中一致
