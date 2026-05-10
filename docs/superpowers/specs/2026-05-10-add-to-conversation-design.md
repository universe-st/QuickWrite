# "添加到对话"功能 — 设计文档

**日期**: 2026-05-10  
**状态**: 已确认  
**关联需求**: 编辑器选中文本后添加到 AI 对话，附加引用信息

---

## 1. 功能概述

在写作编辑器中选中一段文本后，系统弹出文本操作菜单，新增"添加到对话"选项。点击后：
- 若无选中会话 → Toast 提示"请先选择一个对话"
- 若有选中会话 → 自动切换到 Chat Tab，在消息发送框上方添加引用块
- 引用块显示文件路径、内容预览（前两行），支持手动移除，最多5个
- 发送消息时，引用信息自动拼接到消息内容最前面

---

## 2. 数据模型

### 2.1 ReferenceBlock

```kotlin
data class ReferenceBlock(
    val id: String,           // UUID，用作 LazyColumn key 和移除标识
    val filePath: String,     // 相对于项目根目录的路径，如 "正文/第一章.md"
    val contentPreview: String, // 选中文本的前两行（展示用）
    val startLine: Int,       // 文件中的起始行号（1-indexed，含 YAML Front Matter）
    val endLine: Int          // 文件中的结束行号（1-indexed，含 YAML Front Matter）
)
```

### 2.2 状态存放

引用块列表存储在 `WritingViewModel` 的 `WritingUiState.Success` 中：

```kotlin
data class Success(
    // ... 现有字段保持不变
    val referenceBlocks: List<ReferenceBlock> = emptyList()
)
```

`WritingViewModel` 暴露以下方法：

| 方法 | 说明 |
|------|------|
| `addReference(filePath, selectedText, bodyStartLine, bodyEndLine)` | 校验会话存在性 & 数量上限 → 计算文件真实行号 → 添加到 referenceBlocks |
| `removeReference(id)` | 按 id 移除单个引用块 |
| `clearReferences()` | 发送消息后清空全部引用块 |
| `buildMessageWithReferences(userInput): String` | 拼接引用行 + 用户输入 |

### 2.3 生命周期

- 添加：编辑器选中文本 → 点击"添加到对话"
- 移除：用户点击引用块上的 × 按钮
- 清空：发送消息后自动清空全部引用块
- 跨章节：切换章节后保留（引用块与章节无关，属于对话上下文）

---

## 3. 编辑器集成 — 选择菜单新增按钮

### 3.1 修改位置

`markor-editor/src/main/kotlin/com/universe_st/markor_editor/MarkorEditor.kt`

在 `AndroidView` 的 `factory` lambda 中，覆盖 `setCustomSelectionActionModeCallback`，在现有自定义菜单（仅 `☰` 按钮）旁新增"添加到对话"菜单项。

**不修改** `HighlightingEditor.java` 的 `setupCustomOptions()`。

### 3.2 MarkorEditor 新增参数

```kotlin
@Composable
fun MarkorEditor(
    // ... 现有参数
    onAddToConversation: ((selectedText: String, startLine: Int, endLine: Int) -> Unit)? = null
)
```

### 3.3 菜单实现

```kotlin
view.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(0, 0, 0, "☰")            // 保留：全选当前行
        menu?.add(0, 1, 1, "添加到对话")    // 新增
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            0 -> view.selectLines()
            1 -> {
                val start = minOf(view.selectionStart, view.selectionEnd)
                val end = maxOf(view.selectionStart, view.selectionEnd)
                if (start != end) {
                    val text = view.text.substring(start, end)
                    val layout = view.layout ?: return false
                    val startLine = layout.getLineForOffset(start)
                    val endLine = layout.getLineForOffset(end)
                    onAddToConversation?.invoke(text.toString(), startLine, endLine)
                    mode?.finish()  // 关闭菜单
                }
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
})
```

### 3.4 WritingScreen 中的回调连接

```kotlin
MarkorEditor(
    // ... 现有参数
    onAddToConversation = { selectedText, startLine, endLine ->
        viewModel.addReference(
            filePath = currentChapterPath,
            selectedText = selectedText,
            bodyStartLine = startLine,
            bodyEndLine = endLine
        )
    }
)
```

**注意**：`startLine`/`endLine` 为编辑器正文中的行号（0-indexed，不含 YAML Front Matter）。

---

## 4. Front Matter 行号偏移计算

### 4.1 偏移量计算

编辑器显示的正文已剥离 YAML Front Matter，但文件真实行号需包含 Front Matter 行数。

```kotlin
fun calculateFrontMatterLineCount(fullContent: String): Int {
    val lines = fullContent.lines()
    if (lines.isEmpty() || lines[0].trim() != "---") return 0
    val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
    if (endIndex == -1) return 0
    return endIndex + 2  // 首行 --- + 中间元数据行 + 结束 ---
}
```

### 4.2 真实行号换算

```kotlin
val fileStartLine = bodyStartLine + frontMatterLineCount + 1  // 0-indexed → 1-indexed
val fileEndLine   = bodyEndLine   + frontMatterLineCount + 1
```

### 4.3 WritingViewModel.addReference 流程

```kotlin
fun addReference(filePath: String, selectedText: String, bodyStartLine: Int, bodyEndLine: Int) {
    // 1. 检查是否有选中会话
    if (currentSessionId == null) {
        _event.emit(UiEvent.ShowToast(getString(R.string.chat_no_session_selected)))
        return
    }
    // 2. 检查数量上限
    val blocks = _uiState.value.referenceBlocks ?: emptyList()
    if (blocks.size >= 5) {
        _event.emit(UiEvent.ShowToast(getString(R.string.chat_reference_max_reached)))
        return
    }
    // 3. 读取原始文件内容计算 Front Matter 偏移
    val fullContent = readChapterFile(filePath)
    val fmOffset = calculateFrontMatterLineCount(fullContent)
    val fileStartLine = bodyStartLine + fmOffset + 1
    val fileEndLine = bodyEndLine + fmOffset + 1
    // 4. 截取前两行作为预览
    val preview = selectedText.lines().take(2).joinToString("\n")
        .let { if (selectedText.lines().size > 2) "$it..." else it }
    // 5. 添加到列表 & 触发切换到 Chat Tab
    addReferenceBlock(ReferenceBlock(
        id = UUID.randomUUID().toString(),
        filePath = filePath,
        contentPreview = preview,
        startLine = fileStartLine,
        endLine = fileEndLine
    ))
}
```

---

## 5. 对话端 UI — 引用块展示

### 5.1 ChatTab 参数扩展

```kotlin
@Composable
fun ChatTab(
    // ... 现有参数
    referenceBlocks: List<ReferenceBlock> = emptyList(),
    onRemoveReference: (String) -> Unit = {},
    onReferencesCleared: () -> Unit = {}
)
```

### 5.2 引用块区域组件

在 `ChatInputArea`（或 `ChatTab` 中 ChatInputArea 的正上方）新增 `ReferenceBlockList` Composable：

**布局**：垂直列表，每个引用块为一个 Card/Row，最多5个。

**单个引用块结构**：
```
┌──────────────────────────────────────────────┐
│ 引用 正文/第一章.md                     [×]  │
│ 主角醒来发现自己穿越到了异世界。              │
│ 环顾四周，这是一片陌生的森林...              │
└──────────────────────────────────────────────┘
```

- 第一行：`引用 [文件路径]`（MaterialTheme.colorScheme.outline 色，caption 字体）
- 第二、三行：contentPreview 内容（bodyMedium 字体），超出省略号
- 右侧：`IconButton` × 图标，调用 `onRemoveReference(id)`
- 间距：8.dp 垂直间距
- 若列表为空 → 不渲染本区域

### 5.3 消息拼接

在发送消息前（`ChatInputArea` 或 `AiChatViewModel.sendMessage` 处），拼接引用信息：

```kotlin
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
```

示例输出：
```
[引用 正文/第一章.md 12-15]
[引用 正文/第三章.md:45]
评价一下这段开头。
```

发送后立即调用 `onReferencesCleared()` 清空引用块。

### 5.4 WritingScreen 中 ChatTab 的调用

```kotlin
ChatTab(
    projectId = projectId,
    viewModel = aiChatViewModel,
    referenceBlocks = uiState.referenceBlocks,
    onRemoveReference = { id -> viewModel.removeReference(id) },
    onReferencesCleared = { viewModel.clearReferences() }
)
```

### 5.5 切换章节时的行为

- 切换章节 → 引用块保留不变（属于对话上下文，非编辑器上下文）
- 切换项目 → 需清空引用块（会话不跨项目）

---

## 6. 系统提示词更新

### 6.1 修改文件

`app/src/main/assets/prompts/novel_writing_assistant.md`

### 6.2 新增内容

在工具使用说明区域（查看文件部分相邻位置）新增以下章节：

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

### 6.3 影响范围

仅 `novel_writing_assistant.md`，无需修改 `no_project_assistant.md` 和 `default_assistant.md`（引用功能仅在项目上下文中有意义）。

---

## 7. 字符串资源

### 7.1 新增字符串（三个语言文件）

| Key | 英文 (values) | 简体中文 (values-zh-rCN) | 繁体中文 (values-zh-rTW) |
|-----|-------------|------------------------|------------------------|
| `chat_reference_label` | Reference | 引用 | 引用 |
| `chat_reference_max_reached` | Maximum 5 references | 最多添加5个引用 | 最多添加5個引用 |
| `chat_reference_remove` | Remove reference | 移除引用 | 移除引用 |
| `chat_no_session_selected` | Please select a conversation first | 请先选择一个对话 | 請先選擇一個對話 |
| `chat_reference_added` | Reference added | 已添加引用 | 已添加引用 |

### 7.2 菜单标题国际化

编辑器菜单中的"添加到对话"标题需支持多语言。实现方式：

- `MarkorEditor` 新增参数 `addToConversationLabel: String = "添加到对话"`
- 调用方（WritingScreen）通过 `stringResource(R.string.chat_add_to_conversation)` 传入
- markor-editor 模块不直接依赖 app 的 R 资源，保持模块解耦

新增一条字符串资源：

| Key | 英文 (values) | 简体中文 (values-zh-rCN) | 繁体中文 (values-zh-rTW) |
|-----|-------------|------------------------|------------------------|
| `chat_add_to_conversation` | Add to Chat | 添加到对话 | 添加到對話 |

---

## 8. 涉及文件清单

### 修改的文件

| 文件 | 修改内容 |
|------|---------|
| `markor-editor/.../MarkorEditor.kt` | 新增 `onAddToConversation` 回调参数，覆盖 ActionMode.Callback |
| `app/.../presentation/ui/screens/WritingScreen.kt` | 传递 `onAddToConversation` 回调；ChatTab 传入 referenceBlocks 参数 |
| `app/.../presentation/ui/screens/ChatTab.kt` | 新增 referenceBlocks / onRemoveReference / onReferencesCleared 参数；添加 ReferenceBlockList 组件 |
| `app/.../presentation/viewmodel/WritingViewModel.kt` | 新增 addReference / removeReference / clearReferences / buildMessageWithReferences 方法；uiState 新增 referenceBlocks 字段 |
| `app/src/main/assets/prompts/novel_writing_assistant.md` | 新增"引用格式规则"章节 |
| `app/src/main/res/values/strings.xml` | 新增 5 条英文字符串 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 新增 5 条简体中文字符串 |
| `app/src/main/res/values-zh-rTW/strings.xml` | 新增 5 条繁体中文字符串 |

### 新增的文件

无。所有改动在现有文件中完成。

---

## 9. 约束与边界条件

| 条件 | 行为 |
|------|------|
| 无选中会话 | Toast："请先选择一个对话"，不添加引用块 |
| 引用块已达 5 个 | Toast："最多添加5个引用"，不添加 |
| 选中文本为空（无选中或光标单点） | 不显示/不处理（由 `start != end` 判断拦住） |
| 切换章节 | 引用块保留不变 |
| 切换项目 | 引用块清空 |
| 文件无 Front Matter | fmOffset = 0，行号正常计算 |
| 文件 Front Matter 格式异常 | fmOffset = 0（降级处理） |
| 会话被删除 | 引用块保留但下次添加时触发 Toast（sessionId 变为 null） |
| 发送消息时引用块为空 | 不拼接引用行，直接发送原消息 |
| Chat Tab 未渲染时添加引用 | 切换 Tab 时自动带上引用块（通过 uiState 驱动） |

---

## 10. 测试要点

1. 编辑器选中文本 → 菜单出现"添加到对话" → 点击 → Toast"请先选择一个对话"（无会话时）
2. 创建会话 → 选中文本 → 点击"添加到对话" → 自动切到 Chat Tab → 引用块出现
3. 引用块显示正确路径、前两行内容、省略号
4. 点击 × 移除引用块
5. 添加第 6 个引用块 → Toast 提示上限
6. 发送消息 → 消息内容含引用行 → 引用块清空
7. 切换章节 → 引用块保留
8. 切换项目 → 引用块清空（或在新项目中无会话状态下被拦截）
9. Front Matter 计算正确（人工验证文件行号与 ViewFile 工具输出一致）
10. 提示词更新后 AI 能正确理解引用内容
