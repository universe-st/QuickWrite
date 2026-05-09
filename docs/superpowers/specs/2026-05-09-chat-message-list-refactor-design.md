# 对话消息列表重构设计文档

**日期**: 2026-05-09
**状态**: 已批准
**范围**: ChatTab.kt, ChatBubble.kt 的重写

---

## 1. 需求

1. AI 回复和工具调用左对齐，用户消息右对齐（维持现有）
2. AI 生成回复时，气泡内文本下方显示 indeterminate 进度条
3. AI 回复始终使用 `MarkdownText` 渲染（包括流式生成过程中）；用户消息用纯 `Text`
4. 新消息到达时自动滚动到底部
5. SSE 流式返回时逐字显示，若用户在底部则持续自动滚动
6. 用户离开底部时，右下角显示滚动到底部悬浮按钮

---

## 2. 核心架构决策：reverseLayout 自动滚动

采用 `LazyColumn(reverseLayout = true)` 彻底消除手动滚动逻辑。

### 2.1 原理

`reverseLayout = true` 将 LazyColumn 从底部开始布局：
- 索引 0 位于屏幕最底部
- 索引 N 位于屏幕最顶部（用户向上滑动可见）

当索引 0 的内容增长（如 SSE 流式追加文字），底部锚定不动，内容向上扩张——新文字始终可见，**无需任何 `scrollToItem` 调用**。

### 2.2 数据流

```
messages (Room Flow, 时间正序: oldest → newest)
  → preprocessMessages() → displayItems (时间正序)
  → displayItems.asReversed() (倒序: newest → oldest)
  → LazyColumn(reverseLayout = true, items = displayItemsReversed)
  → 索引 0 = 最新消息, 显示在屏幕底部 ✓
```

### 2.3 流式生成气泡的位置

生成中的气泡放在 `displayItemsReversed` 的索引 0（即最新位置），`reverseLayout` 确保它锚定在屏幕底部，文字逐字增长时自动扩张。

### 2.4 滚动到底 FAB

仅需一个简单判断：
```kotlin
val showFAB by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

`firstVisibleItemIndex > 0` 表示用户已向上滚动离开最新消息。点击 FAB 执行 `listState.animateScrollToItem(0)`。

---

## 3. UI 布局结构

```
ChatContentArea
├── ErrorBanner（SessionState.Error 时显示，含重试按钮）
├── Box(weight = 1f)                          // 消息列表区域
│   └── LazyColumn(reverseLayout = true)
│       ├── item(key = "generating")           // [仅在 isGenerating 时]
│       │   └── GeneratingBubble               // 流式生成气泡
│       │       ├── MarkdownText(partialContent)
│       │       └── LinearProgressIndicator(indeterminate)  // 有内容时显示
│       ├── itemsIndexed(displayItemsReversed) // 已完成的工具卡/消息
│       │   ├── ToolCard → ToolExecutionCard
│       │   ├── USER → UserMessageBubble (Text, 右对齐)
│       │   ├── ASSISTANT → AssistantMessageBubble (MarkdownText, 左对齐)
│       │   └── SYSTEM → SystemMessageBubble (居中淡色)
│       └── item(key = "empty_hint")           // [无消息时] 空状态提示
│   └── ScrollToBottomFAB                     // showFAB 时显示
├── ChatInputArea                              // 输入框+发送/停止按钮
```

---

## 4. 组件详细设计

### 4.1 ChatTab.kt — ChatContentArea 重写

**移除的复杂状态**（约 150 行）：
- `followBottom` / `currentFollowBottom`
- `isProgrammaticScroll` / `currentIsProgrammaticScroll`
- `isScrollingToBottom` / `currentIsScrollingToBottom`
- 所有的 `snapshotFlow { isAtBottom to isScrollInProgress }`
- `LaunchedEffect(displayItems.size, messages.size)` 含复杂滚动逻辑
- `LaunchedEffect(partialContent)` 含 `withFrameNanos`
- `isAtBottom` derivedState
- `scrollToBottom()` 辅助函数

**新增的简单逻辑**：
- `val showFAB = derivedStateOf { listState.firstVisibleItemIndex > 0 }`
- `val displayItemsReversed = displayItems.asReversed()`
- FAB onClick: `listState.animateScrollToItem(0)`
- `LaunchedEffect(currentSessionId)`: 新会话时滚动到 0

**preprocessMessages 重写**：
- 输入：`messages: List<ChatMessage>` + `isGenerating: Boolean`
- 输出：`List<DisplayItem>`（不含流式气泡，只含已完成的消息和工具卡）
- TOOL 消息合并到其前置 ASSISTANT 的工具卡中（逻辑不变）
- 生成中的 pseudo-message (`id = Long.MAX_VALUE`) 移除，改为独立的 GeneratingBubble item

**DisplayItem 密封类变更**：
```kotlin
private sealed class DisplayItem {
    data class Message(val message: ChatMessage) : DisplayItem()
    data class ToolCard(
        val toolName: String,
        val parsed: ToolResultParsed?,
        val isLoading: Boolean
    ) : DisplayItem()
    // 移除 pseudo-message 用法，GenerateBubble 作为 LazyColumn 的独立 item
}
```

### 4.2 ChatBubble.kt — 组件变更

**AssistantMessageBubble 重写**：

```kotlin
@Composable
fun AssistantMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            MarkdownText(markdown = content)  // 始终使用 MarkdownText
        }
    }
}
```

移除 `isGenerating` 参数、`PulsingCursor`、`Text()` 分支。AI 回复在所有状态下统一使用 `MarkdownText`。

**新增 GeneratingBubble**：

```kotlin
@Composable
fun GeneratingBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (content.isNotEmpty()) {
                    MarkdownText(markdown = content)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LoadingPlaceholder()  // 首字到达前显示 "AI is typing..."
                }
            }
        }
    }
}
```

**保留并可能清除的组件**：
- `PulsingCursor` — 移除（不再需要）
- `TypingIndicator` — 移除（被 LoadingPlaceholder 取代）
- `LoadingPlaceholder` — 保留，GenerateBubble 空内容时显示
- `ToolCallBubble` — 标记 @Deprecated（已被 ToolExecutionCard 取代）
- `ToolResultBubble` — 标记 @Deprecated（已被 ToolExecutionCard 取代）

**MessageBubble 传递路径简化**：
- USER → `UserMessageBubble(content, showActions, ...)` — 不变
- ASSISTANT（已完成）→ `AssistantMessageBubble(content)` — 简化，移除 isGenerating/toolCalls 参数
- TOOL → 空（工具结果已在 displayItems 中合并为 ToolCard）
- SYSTEM → `SystemMessageBubble(content)` — 不变

### 4.3 ToolExecutionCard.kt — 无需变更

### 4.4 ViewModel / Data Layer — 无需变更

`SessionManager`, `ApiDispatcher`, `AiChatViewModel` 的 API 一致，仅 UI 层消费方式改变。

---

## 5. 滚动行为详述

| 场景 | 行为 |
|------|------|
| 新会话加载 | `firstVisibleItemIndex = 0`（底部），FAB 不显示 |
| 用户发送消息 | 新 USER 消息进入 displayItems → asReversed 后出现在索引 0 → reverseLayout 自动显示在底部 |
| AI 开始生成 | GeneratingBubble 出现在索引 0，空内容时显示 LoadingPlaceholder |
| SSE 逐字到达 | `partialContent` 变化 → GeneratingBubble 重组 → 高度增长 → reverseLayout 底部锚定 → 新文字自动可见 |
| AI 生成完成 | GeneratingBubble 移除，最终 ASSISTANT 消息 + 工具卡替换到 displayItems 中 |
| 用户向上滑动看历史 | `firstVisibleItemIndex > 0` → FAB 出现 |
| 用户点击 FAB | `animateScrollToItem(0)` → 平滑滚动回底部 → FAB 消失 |
| 有新消息但用户在看历史 | 消息加到索引 0，但 `firstVisibleItemIndex > 0` 未变 → 不打断用户，FAB 保持显示 |

---

## 6. 移除的代码清单

### ChatTab.kt 中移除：
- `isScrollingToBottom` mutableState 及相关的 `onScrollToBottomStart/End` 回调
- `followBottom` mutableState
- `isProgrammaticScroll` mutableState
- `isAtBottom` derivedState
- `scrollToBottom()` suspend 函数
- `snapshotFlow { isAtBottom to isScrollInProgress }` LaunchedEffect
- `LaunchedEffect(currentSessionId, displayItems.size, messages.size)` 
- `LaunchedEffect(partialContent)` 含 withFrameNanos
- 两个 `Box(modifier = Modifier.matchParentSize().zIndex().pointerInput(...))` 阻挡触摸层
- pseudo-message `ChatMessage(id = Long.MAX_VALUE, ...)` 构建逻辑

### ChatBubble.kt 中移除：
- `AssistantMessageBubble` 的 `isGenerating` 参数
- `AssistantMessageBubble` 的 `toolCalls` 参数
- `PulsingCursor` Composable
- `TypingIndicator` Composable
- `ToolCallBubble` → @Deprecated
- `ToolResultBubble` → @Deprecated

---

## 7. 潜在风险与对策

| 风险 | 对策 |
|------|------|
| `reverseLayout` 下 padding 行为异常（top/bottom 颠倒） | 在 `reverseLayout` 的 LazyColumn 中，`contentPadding` 的 `top` 实际作用于视觉底部。不使用 contentPadding，改为在每个 item 上手动设置 padding |
| `firstVisibleItemIndex` 语义与常规布局不同 | 充分注释说明 reverseLayout 下索引方向，必要时添加调试日志 |
| 工具调用期间流式中断再恢复时显示错乱 | `handleToolCalls` 中的 `delay(150)` 确保 UI 先观察到工具卡，再执行工具、获取结果、更新卡状态 |
| `asReversed()` 每次重组都创建新列表 | 使用 `remember(messages, isGenerating) { ... }` 缓存 transform 结果 |

---

## 8. 文件变更清单

| 文件 | 变更类型 | 描述 |
|------|---------|------|
| `presentation/ui/screens/ChatTab.kt` | 重写 | ChatContentArea 核心逻辑重写，移除复杂滚动状态 |
| `presentation/ui/components/ChatBubble.kt` | 重写 | AssistantMessageBubble 简化，新增 GeneratingBubble，清理废弃组件 |

---

## 9. 验证方式

```bash
# 编译验证
./gradlew :app:assembleDebug

# 功能验证清单
- [ ] 新消息到达时自动滚动到底部
- [ ] SSE 流式文字逐字显示，在底部时自动跟随
- [ ] 上滑看历史时不被打断，FAB 出现
- [ ] 点击 FAB 平滑滚动到底部
- [ ] AI 回复使用 MarkdownText 渲染（含流式生成中）
- [ ] 用户消息纯文本右对齐
- [ ] 生成中气泡内显示进度条
- [ ] 工具调用卡片正常显示
- [ ] 空状态提示正常
- [ ] 错误横幅正常
- [ ] 会话切换正常
