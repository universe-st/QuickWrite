# AI 工具调用视图设计文档

## 文档概述

本文档定义 AI 对话中 **工具调用（Tool Call）的 UI 展示方案**。目标是将原始的 JSON 工具调用参数和结果，转换为用户友好的可视化卡片，支持加载动画、执行统计、内容展开等功能。

**状态**: 设计阶段  
**版本**: 1.0  
**创建日期**: 2026-05-01

---

## 1. 设计目标

### 1.1 用户体验目标

| 目标 | 说明 |
|------|------|
| **工具名称本地化** | `create_file` → "创建文件"，`edit_file` → "编辑文件"，用户无需了解 API 内部命名 |
| **隐藏技术参数** | 不再展示 JSON 参数列表，减少用户认知负担 |
| **执行状态可视化** | 工具执行期间显示 LOADING 动画，让用户感知"AI 正在工作" |
| **结果统计展示** | 完成后显示字数、行数、文件大小等关键统计 |
| **点击查看详情** | 点击卡片可展开查看写入/编辑/读取的具体内容 |
| **成功/失败状态** | 清晰的视觉区分，失败时显示错误摘要 |

### 1.2 技术目标

| 目标 | 说明 |
|------|------|
| **向后兼容** | 不改变现有 `ChatMessage`/`AiMessageEntity` 数据模型，仅增强 UI 层渲染 |
| **解析工具结果** | 从 TOOL 消息的 JSON 结果中提取结构化信息用于展示 |
| **解耦工具名与显示名** | 通过映射表集中管理，方便未来扩展新工具和多语言 |
| **保留原始数据** | 卡片可展开查看原始 JSON 参数/结果，供开发者调试 |

---

## 2. 当前实现分析

### 2.1 现有消息流

```
用户发送消息
  └─ ApiDispatcher.performSendMessage()
       ├─ 发送 API 请求（含 tools 定义）
       ├─ 流式解析响应
       ├─ AI 返回 text + toolCalls → 持久化 ASSISTANT 消息（含 toolCallsJson）
       ├─ 逐个执行工具
       │   ├─ SessionState.Generating("Executing tool: {name}...")  ← 仅文本状态
       │   ├─ ToolExecutor.executeToolCall()
       │   └─ 持久化 TOOL 消息（结果 JSON）
       └─ 递归继续对话
```

### 2.2 现有 UI 组件

#### ToolCallBubble（ChatBubble.kt:204-278）
- 位置：ASSISTANT 消息气泡下方
- 显示：`"Tool Calls: N"` 标题 + 每个工具的函数名（驼峰命名分割）
- 交互：点击展开/折叠 JSON 参数
- 颜色：`tertiaryContainer` 半透明
- **问题**：无加载状态、无执行结果统计、展示原始 JSON 参数

#### ToolResultBubble（ChatBubble.kt:282-309）
- 位置：独立 TOOL 消息
- 显示：截断的 JSON 文本（300 字符）
- 交互：无
- **问题**：无结构化展示、无统计信息、无法查看完整内容

### 2.3 所有工具及其返回格式

| 工具名 | 类型 | 返回字段 |
|--------|------|---------|
| `create_file` | 写入 | `created`, `path`, `size` |
| `edit_file` | 写入 | `filePath`, `replacedRange`, `newContent`, `lineCountAfter` |
| `delete_file` | 写入 | `deleted`, `path` |
| `move_file` | 写入 | `moved`, `sourcePath`, `targetPath` |
| `copy_file` | 写入 | `copied`, `sourcePath`, `targetPath`, `size` |
| `create_project` | 写入 | `created`, `projectId`, `title`, `storagePath` |
| `delete_project` | 写入 | `deleted`, `projectId`, `title` |
| `update_project_info` | 写入 | `updated`, `projectId`, `changedFields` |
| `view_file` | 只读 | `filePath`, `totalLines`, `startLine`, `endLine`, `language`, `content` |
| `search_in_project` | 只读 | `query`, `results`[], `resultCount` |
| `get_project_list` | 只读 | `projects`[], `count` |
| `get_project_info` | 只读 | `id`, `title`, `author`, `genre`, `status`, `wordCount`, `chapterCount`, `directoryStats` |
| `get_folder_structure` | 只读 | `path`, `entries`[] |

---

## 3. 整体架构

### 3.1 显示名称映射表

创建 `ToolDisplayInfo.kt` 集中管理工具到 UI 展示信息的映射：

```kotlin
data class ToolDisplayInfo(
    val toolName: String,          // API 工具名，如 "create_file"
    val displayNameRes: Int,       // 显示名称字符串资源 ID
    val displayNameZh: String,     // 中文显示名称
    val loadingTextRes: Int,       // 加载中文案字符串资源 ID  
    val loadingTextZh: String,     // 加载中提示文本
    val icon: ToolIcon,            // 图标类型
    val category: ToolCategory     // 分类：读取/写入/管理
)

enum class ToolIcon { FILE_ADD, FILE_EDIT, FILE_DELETE, FILE_MOVE, 
                       SEARCH, FOLDER, PROJECT, INFO }
enum class ToolCategory { READ, WRITE, MANAGE }
```

### 3.2 数据流变更

```
现有流程:
  Assistant 消息到达 → ToolCallBubble（显示计划调用）
  Tool 消息到达 → ToolResultBubble（显示 JSON 结果）

新流程:
  Assistant 消息到达（含 toolCalls）
    └─ ToolExecutionCard（加载态）
         ├─ 图标 + 显示名称
         ├─ 动画加载指示器
         └─ "正在执行..." 文本

  Tool 消息到达（对应 toolCallId）
    └─ ToolExecutionCard（完成态）
         ├─ 图标 + 显示名称
         ├─ 成功/失败标记
         ├─ 统计信息（字数、行数等）
         └─ [点击展开] 详细内容
```

### 3.3 消息合并策略

**原则**：数据库不做改动，仅 UI 层重构消息列表显示。

在 `ChatTab` 的消息列表准备阶段，将连续的 `ASSISTANT(含toolCalls) + TOOL×N` 消息合并为一个 `DisplayItem` 列表项：

```
原始消息序列:
  [USER] "帮我创建人物设定"
  [ASSISTANT] content="好的" + toolCalls=[create_file(path="设定/人物/主角.md", content="...")]
  [TOOL] {"created": true, "path": "设定/人物/主角.md", "size": 1234}
  [ASSISTANT] "已完成，以下是创建的文件" + toolCalls=[view_file(path="设定/人物/主角.md")]
  [TOOL] {"filePath": "...", "totalLines": 45, "content": "..."}
  [ASSISTANT] "文件内容如上，需要调整吗？"

显示为:
  [USER Bubble] "帮我创建人物设定"
  [Assistant Bubble] "好的"
  [Tool Card — 创建文件 · 1234 字 ✓ · 可展开]
  [Assistant Bubble] "已完成，以下是创建的文件"
  [Tool Card — 查看文件 · 45 行 ✓ · 可展开查看内容]
  [Assistant Bubble] "文件内容如上，需要调整吗？"
```

---

## 4. 各工具视图设计

### 4.1 通用卡片结构

```
┌─────────────────────────────────────────────────┐
│ 🔵 [图标]  [显示名称]              [状态标记]    │  ← 标题行
│            [统计信息: N 行 · N 字]              │  ← 统计行（完成态）
│            [加载动画: ⠋ 正在创建...]            │  ← 加载行（加载态）
│            ┌─────────────────────────────────┐  │
│            │  展开内容区域（点击展开）          │  │  ← 可折叠内容区
│            │  （文件内容 / 搜索结果 / ...）    │  │
│            └─────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**颜色方案**：
| 状态 | 容器色 | 图标色 |
|------|--------|--------|
| 加载中 | `tertiaryContainer` 50%透明度 + 主题色边框 | 动画渐变 |
| 成功 | `primaryContainer` 30%透明度 | 主题色 |
| 失败 | `errorContainer` | `error` |

**尺寸**：
- 卡片圆角：8dp
- 左侧缩进：48dp（与 ASSISTANT 消息视觉关联）
- 标题行内边距：horizontal 12dp, vertical 8dp
- 展开内容区内边距：horizontal 12dp, vertical 8dp

### 4.2 写入类工具

#### 4.2.1 create_file（创建文件）

```
┌─────────────────────────────────────────────────┐
│ 📄 创建文件                              ✓ 完成  │
│    45 行 · 1,234 字                              │
│    ┌─────────────────────────────────────────┐  │
│    │ # 主角                                     │
│    │ 姓名：林风                                 │
│    │ 年龄：24 岁                                │
│    │ ...                                       │
│    └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在创建文件..."`
**完成态统计**：行数（从 content 计算）、文件大小（从 result.size）
**展开内容**：文件完整内容（Markdown 格式渲染）
**失败态**：显示错误信息摘要

**参数提取逻辑**：
```kotlin
// 从 ASSISTANT 消息的 toolCalls 中提取 content 参数计算行数和字数
val content = call.function.arguments.getContent()  // 从 JSON 中提取
val lineCount = content.lines().size
val charCount = content.length
```

#### 4.2.2 edit_file（编辑文件）

```
┌─────────────────────────────────────────────────┐
│ ✏️ 编辑文件                              ✓ 完成  │
│    替换第 12-18 行 · 共 67 行                    │
│    ┌─────────────────────────────────────────┐  │
│    │ - 旧版本内容摘要...                        │
│    │ + 新版本内容                              │
│    └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在编辑文件..."`
**完成态统计**：替换范围（`replacedRange`）、文件总行数（`lineCountAfter`）
**展开内容**：`newContent`（新写入内容）
**失败态**：显示错误信息（如 "File not found"）

#### 4.2.3 delete_file（删除文件）

```
┌─────────────────────────────────────────────────┐
│ 🗑 删除文件                              ✓ 完成  │
│    已删除: 设定/人物/废弃角色.md                   │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在删除文件..."`
**完成态统计**：`path`
**展开内容**：无需展开（已删除，无可展示内容）
**失败态**：显示错误（如 "Cannot delete core directory"）

#### 4.2.4 move_file（移动/重命名文件）

```
┌─────────────────────────────────────────────────┐
│ 📁 移动文件                              ✓ 完成  │
│    设定/人物/主角.md → 设定/人物/主角设定.md       │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在移动文件..."`
**完成态统计**：`sourcePath → targetPath`
**展开内容**：无需展开
**失败态**：显示错误信息

#### 4.2.5 copy_file（复制文件）

```
┌─────────────────────────────────────────────────┐
│ 📋 复制文件                              ✓ 完成  │
│    设定/人物/主角.md → 设定/人物/主角_备份.md     │
│    文件大小: 2,048 字节                           │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在复制文件..."`
**完成态统计**：`sourcePath → targetPath`、`size`
**展开内容**：无需展开

### 4.3 项目管理类工具

#### 4.3.1 create_project（创建项目）

```
┌─────────────────────────────────────────────────┐
│ 🏗 创建项目                              ✓ 完成  │
│    项目名称：《星辰大海》                          │
│    项目 ID: abc123-def456                        │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在创建项目..."`
**完成态统计**：`title`、`projectId`
**展开内容**：项目完整信息（title、author、genre、storagePath）

#### 4.3.2 delete_project（删除项目）

```
┌─────────────────────────────────────────────────┐
│ 🗑 删除项目                              ✓ 完成  │
│    已删除: 《星辰大海》(abc123-def456)            │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在删除项目..."`
**完成态统计**：`title`、`projectId`
**展开内容**：无需展开
**失败态**：显示错误（如 "Title confirmation failed"）

#### 4.3.3 update_project_info（更新项目信息）

```
┌─────────────────────────────────────────────────┐
│ ⚙️ 更新项目信息                          ✓ 完成  │
│    已更新: 标题、类型、简介                        │
│    ┌─────────────────────────────────────────┐  │
│    │ 标题: 星辰大海 → 星辰大海：起源             │
│    │ 类型: 玄幻 → 科幻                         │
│    └─────────────────────────────────────────┘  │
```

**加载态**：`"正在更新项目信息..."`
**完成态统计**：变更字段列表（从 `changedFields` 提取中文映射）
**展开内容**：字段变更详情（before → after）
**失败态**：显示错误或 "No fields changed"

### 4.4 读取类工具

#### 4.4.1 view_file（查看文件）

```
┌─────────────────────────────────────────────────┐
│ 👁 查看文件                              ✓ 完成  │
│    设定/人物/主角.md · 共 45 行                   │
│    ┌─────────────────────────────────────────┐  │
│    │ 1: # 主角                                │
│    │ 2: 姓名：林风                            │
│    │ 3: 年龄：24 岁                           │
│    │ ...                                     │
│    └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在读取文件..."`
**完成态统计**：`filePath`、`totalLines`
**展开内容**：文件完整内容（带行号的 Markdown 渲染）
**注**：如果 `truncated=true`，显示 `"内容已截断，显示 500/1200 行"`

#### 4.4.2 search_in_project（搜索文件）

```
┌─────────────────────────────────────────────────┐
│ 🔍 搜索文件                              ✓ 完成  │
│    搜索 "主角设定" · 找到 3 个结果                │
│    ┌─────────────────────────────────────────┐  │
│    │ 📄 设定/人物/主角.md:12                   │
│    │    主角设定：姓名林风，24岁              │
│    │ 📄 设定/人物/反派.md:5                    │
│    │    与主角设定相关...                     │
│    │ 📄 正文/第一章.md:42                      │
│    │    主角设定完整...                       │
│    └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**加载态**：`"正在搜索..."`
**完成态统计**：`query`、`resultCount`
**展开内容**：搜索结果列表（文件路径 + 行号 + 匹配行内容）
**截断提示**：`"结果已截断，仅显示前 20 条"`

#### 4.4.3 get_project_list（获取项目列表）

```
┌─────────────────────────────────────────────────┐
│ 📚 获取项目列表                          ✓ 完成  │
│    共 3 个项目                                   │
│    ┌─────────────────────────────────────────┐  │
│    │ 1. 《星辰大海》· 玄幻 · 连载中            │
│    │ 2. 《龙族传奇》· 奇幻 · 完结              │
│    │ 3. 《都市风云》· 现代 · 创作中            │
│    └─────────────────────────────────────────┘  │
```

**加载态**：`"正在获取项目列表..."`
**完成态统计**：`count`
**展开内容**：项目摘要列表（id、title、author、genre、status）

#### 4.4.4 get_project_info（获取项目信息）

```
┌─────────────────────────────────────────────────┐
│ ℹ️ 项目详情                              ✓ 完成  │
│    《星辰大海》· 张三 · 18 章 · 56,000 字         │
│    ┌─────────────────────────────────────────┐  │
│    │ 📊 目录统计                              │
│    │ 正文/ · 18 个文件 · 560 KB               │
│    │ 设定/ · 12 个文件 · 120 KB               │
│    │ 时间线/ · 2 个文件 · 8 KB                │
│    └─────────────────────────────────────────┘  │
```

**加载态**：`"正在获取项目信息..."`
**完成态统计**：`title`、`author`、`chapterCount`、`wordCount`
**展开内容**：目录统计表、项目元数据详情

#### 4.4.5 get_folder_structure（查看目录结构）

```
┌─────────────────────────────────────────────────┐
│ 📂 目录结构                              ✓ 完成  │
│    路径: / · 共 15 项                            │
│    ┌─────────────────────────────────────────┐  │
│    │ 📁 正文/ (3)                             │
│    │   📄 第一章.md (45 KB)                   │
│    │   📄 第二章.md (32 KB)                   │
│    │ 📁 设定/ (12)                            │
│    │   📁 人物/ (8)                          │
│    │     📄 主角.md (5.2 KB)                  │
│    │   📁 地点/ (4)                          │
│    │ 📁 时间线/ (2)                           │
│    └─────────────────────────────────────────┘  │
```

**加载态**：`"正在读取目录结构..."`
**完成态统计**：`path`、总项数
**展开内容**：树形目录结构（目录带 (N) 文件数标记）

---

## 5. 数据层设计

### 5.1 新增领域模型

#### ToolExecutionResult（工具执行结果解析）

```kotlin
// domain/model/ToolDisplayModels.kt

/**
 * 从 TOOL 消息的 JSON 结果中提取的、供 UI 显示的结构化信息
 */
sealed class ToolExecutionResult {
    /** 通用成功结果 */
    data class Success(
        val toolName: String,
        val displayInfo: ToolDisplayInfo,
        val summary: String,                  // 一行摘要文本
        val detailLines: List<String> = emptyList(),  // 详细信息行
        val expandableContent: String? = null,        // 可展开的内容（如文件内容）
        val expandableItems: List<ExpandableItem>? = null, // 列表形式的展开内容
        val stats: List<StatItem> = emptyList(),   // 统计项
        val truncated: Boolean = false,             // 内容是否被截断
        val truncatedMessage: String? = null        // 截断说明
    ) : ToolExecutionResult()

    /** 通用错误结果 */
    data class Error(
        val toolName: String,
        val displayInfo: ToolDisplayInfo,
        val errorSummary: String              // 用户友好的错误摘要
    ) : ToolExecutionResult()

    /** 加载中状态 */
    data class Loading(
        val toolName: String,
        val displayInfo: ToolDisplayInfo
    ) : ToolExecutionResult()
}

data class StatItem(
    val label: String,      // "行数"
    val value: String       // "45"
)

data class ExpandableItem(
    val title: String,      // "设定/人物/主角.md"
    val subtitle: String?,  // "第 12 行"
    val content: String     // 匹配行内容（用于搜索）
)
```

#### ToolDisplayInfo（工具展示信息）

```kotlin
// domain/model/ToolDisplayModels.kt

data class ToolDisplayInfo(
    val toolName: String,       // "create_file"
    val displayName: String,    // "创建文件" (从 resources 获取)
    val loadingText: String,    // "正在创建文件..." (从 resources 获取)
    val iconType: ToolIconType,
    val category: ToolCategory
)

enum class ToolIconType {
    FILE_ADD,        // create_file
    FILE_EDIT,       // edit_file
    FILE_DELETE,     // delete_file
    FILE_MOVE,       // move_file
    FILE_COPY,       // copy_file
    SEARCH,          // search_in_project
    FOLDER_OPEN,     // get_folder_structure
    FOLDER_TREE,     // get_project_list
    INFO,            // get_project_info
    PROJECT_ADD,     // create_project
    PROJECT_DELETE,  // delete_project
    PROJECT_EDIT,    // update_project_info
    EYE              // view_file
}

enum class ToolCategory { READ, WRITE, MANAGE }
```

### 5.2 工具结果解析器

```kotlin
// util/ToolResultParser.kt

class ToolResultParser {

    /**
     * 将 TOOL 消息的 JSON 结果 + 对应 ToolCall 的信息
     * 解析为结构化的 ToolExecutionResult
     */
    fun parse(
        toolName: String,
        resultJson: String,
        toolCallArguments: String?,
        previousMessages: List<ChatMessage> // 用于查找 toolCallId 对应信息
    ): ToolExecutionResult {
        // 1. 从注册表获取 ToolDisplayInfo
        // 2. 尝试解析 JSON
        // 3. 如果是错误 JSON → 返回 Error
        // 4. 如果是成功 JSON → 按工具类型提取统计和可展开内容
        // 5. 返回 Success
    }

    private fun parseCreateFile(result: JSONObject, arguments: String?): ToolExecutionResult.Success
    private fun parseEditFile(result: JSONObject): ToolExecutionResult.Success
    private fun parseDeleteFile(result: JSONObject): ToolExecutionResult.Success
    private fun parseMoveFile(result: JSONObject): ToolExecutionResult.Success
    // ... 每个工具一个解析方法
}
```

### 5.3 数据库不变更

现有 `AiMessageEntity` 结构不变：
- `toolCallsJson`（ASSISTANT 消息）— 保留，含原始调用参数
- `content`（TOOL 消息）— 保留，含原始返回 JSON

解析仅在 UI 层进行，不写入数据库。

---

## 6. UI 组件设计

### 6.1 文件变更

| 文件 | 操作 | 说明 |
|------|------|------|
| `domain/model/ToolDisplayModels.kt` | **新增** | ToolExecutionResult、ToolDisplayInfo、StatItem 等 |
| `util/ToolResultParser.kt` | **新增** | 工具结果 JSON 解析器 |
| `util/ToolDisplayRegistry.kt` | **新增** | 工具展示信息注册表 |
| `presentation/ui/components/ToolExecutionCard.kt` | **新增** | 统一工具执行卡片组件 |
| `presentation/ui/components/ChatBubble.kt` | **修改** | 重构 ToolCallBubble / ToolResultBubble → 委托到 ToolExecutionCard |
| `presentation/ui/screens/ChatTab.kt` | **修改** | 消息列表预处理：合并 ASSISTANT+TOOL → DisplayItem |
| `res/values/strings.xml` | **修改** | 新增工具展示相关字符串（三语言） |
| `res/values-zh-rCN/strings.xml` | **修改** | 同上 |
| `res/values-zh-rTW/strings.xml` | **修改** | 同上 |

### 6.2 ToolExecutionCard 组件

```kotlin
/**
 * 统一的工具执行卡片
 *
 * 根据 [result] 的状态渲染不同的外观：
 * - Loading: 加载动画 + 工具名称
 * - Success: 统计信息 + 可展开内容
 * - Error: 错误摘要
 */
@Composable
fun ToolExecutionCard(
    result: ToolExecutionResult,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp, end = 12.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when (result) {
                is ToolExecutionResult.Loading -> 
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                is ToolExecutionResult.Success -> 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                is ToolExecutionResult.Error -> 
                    MaterialTheme.colorScheme.errorContainer
            },
            border = when (result) {
                is ToolExecutionResult.Loading -> 
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                else -> null
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 标题行
                ToolCardHeader(result)
                
                // 统计/加载/错误行
                when (result) {
                    is ToolExecutionResult.Loading -> ToolCardLoading(result)
                    is ToolExecutionResult.Success -> ToolCardSuccess(result)
                    is ToolExecutionResult.Error -> ToolCardError(result)
                }
            }
        }
    }
}
```

### 6.3 ChatTab 消息预处理逻辑

```kotlin
/**
 * 将原始消息列表转换为 UI 显示列表
 * 合并连续的 [ASSISTANT(含toolCalls) + TOOL×N] 为统一的 DisplayItem
 */
sealed class DisplayItem {
    data class Message(val message: ChatMessage) : DisplayItem()
    data class ToolExecution(
        val toolName: String,               // 从 ASSISTANT.toolCalls 获取
        val toolCallId: String,
        val result: ToolExecutionResult     // 初始为 Loading，TOOL 消息到达后更新为 Success/Error
    ) : DisplayItem()
}

fun prepareDisplayItems(
    messages: List<ChatMessage>,
    partialContent: String?,
    isGenerating: Boolean,
    pendingToolNames: Map<String, String>,  // toolCallId → functionName
    toolResults: Map<String, String>,       // toolCallId → result JSON
    toolArgs: Map<String, String>           // toolCallId → arguments JSON
): List<DisplayItem>
```

### 6.4 加载动画

```kotlin
@Composable
fun ToolCardLoading(result: ToolExecutionResult.Loading) {
    val infiniteTransition = rememberInfiniteTransition(label = "tool_loading")
    val dots = "...".take(infiniteTransition.cycleCount % 3 + 1)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        // 旋转指示器
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = result.displayInfo.loadingText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
```

---

## 7. 字符串资源

### 7.1 工具显示名称（三语言）

| Key | EN | zh-CN | zh-TW |
|-----|-----|-------|-------|
| `tool_name_create_file` | Create File | 创建文件 | 建立檔案 |
| `tool_name_edit_file` | Edit File | 编辑文件 | 編輯檔案 |
| `tool_name_delete_file` | Delete File | 删除文件 | 刪除檔案 |
| `tool_name_move_file` | Move File | 移动文件 | 移動檔案 |
| `tool_name_copy_file` | Copy File | 复制文件 | 複製檔案 |
| `tool_name_create_project` | Create Project | 创建项目 | 建立專案 |
| `tool_name_delete_project` | Delete Project | 删除项目 | 刪除專案 |
| `tool_name_update_project_info` | Update Project Info | 更新项目信息 | 更新專案資訊 |
| `tool_name_view_file` | View File | 查看文件 | 檢視檔案 |
| `tool_name_search_in_project` | Search Files | 搜索文件 | 搜尋檔案 |
| `tool_name_get_project_list` | Get Project List | 获取项目列表 | 取得專案列表 |
| `tool_name_get_project_info` | Project Info | 项目详情 | 專案詳情 |
| `tool_name_get_folder_structure` | View Directory | 查看目录结构 | 檢視目錄結構 |

### 7.2 加载文案（三语言）

| Key | EN | zh-CN | zh-TW |
|-----|-----|-------|-------|
| `tool_loading_create_file` | Creating file... | 正在创建文件... | 正在建立檔案... |
| `tool_loading_edit_file` | Editing file... | 正在编辑文件... | 正在編輯檔案... |
| `tool_loading_delete_file` | Deleting file... | 正在删除文件... | 正在刪除檔案... |
| `tool_loading_move_file` | Moving file... | 正在移动文件... | 正在移動檔案... |
| `tool_loading_copy_file` | Copying file... | 正在复制文件... | 正在複製檔案... |
| `tool_loading_create_project` | Creating project... | 正在创建项目... | 正在建立專案... |
| `tool_loading_delete_project` | Deleting project... | 正在删除项目... | 正在刪除專案... |
| `tool_loading_update_project_info` | Updating project info... | 正在更新项目信息... | 正在更新專案資訊... |
| `tool_loading_view_file` | Reading file... | 正在读取文件... | 正在讀取檔案... |
| `tool_loading_search_in_project` | Searching... | 正在搜索... | 正在搜尋... |
| `tool_loading_get_project_list` | Loading project list... | 正在获取项目列表... | 正在取得專案列表... |
| `tool_loading_get_project_info` | Loading project info... | 正在获取项目信息... | 正在取得專案資訊... |
| `tool_loading_get_folder_structure` | Reading directory... | 正在读取目录结构... | 正在讀取目錄結構... |

### 7.3 通用工具字符串

| Key | EN | zh-CN | zh-TW |
|-----|-----|-------|-------|
| `tool_stat_lines` | %1\$d lines | %1\$d 行 | %1\$d 行 |
| `tool_stat_chars` | %1\$s chars | %1\$s 字 | %1\$s 字 |
| `tool_stat_bytes` | %1\$s bytes | %1\$s 字节 | %1\$s 位元組 |
| `tool_stat_items` | %1\$d items | %1\$d 项 | %1\$d 項 |
| `tool_stat_results` | %1\$d results | %1\$d 个结果 | %1\$d 個結果 |
| `tool_status_loading` | Executing... | 执行中... | 執行中... |
| `tool_status_success` | Completed | 已完成 | 已完成 |
| `tool_status_failed` | Failed | 失败 | 失敗 |
| `tool_truncated` | Content truncated (%1\$d/%2\$d shown) | 内容已截断（显示 %1\$d/%2\$d） | 內容已截斷（顯示 %1\$d/%2\$d） |
| `tool_click_to_expand` | Tap to view content | 点击查看内容 | 點擊檢視內容 |
| `tool_click_to_collapse` | Tap to hide | 点击收起 | 點擊收起 |

---

## 8. 图标设计

### 8.1 图标规格

使用 Material Icons 默认图标集，无需额外依赖：

| 图标类型 | Material Icon | 说明 |
|----------|---------------|------|
| `FILE_ADD` | `Icons.Outlined.NoteAdd` | 添加文件 |
| `FILE_EDIT` | `Icons.Outlined.Edit` | 编辑文件 |
| `FILE_DELETE` | `Icons.Outlined.DeleteOutline` | 删除文件 |
| `FILE_MOVE` | `Icons.Outlined.DriveFileMove` | 移动文件 |
| `FILE_COPY` | `Icons.Outlined.ContentCopy` | 复制文件 |
| `SEARCH` | `Icons.Outlined.Search` | 搜索 |
| `FOLDER_OPEN` | `Icons.Outlined.FolderOpen` | 打开目录 |
| `FOLDER_TREE` | `Icons.Outlined.AccountTree` | 树形结构 |
| `INFO` | `Icons.Outlined.Info` | 信息 |
| `PROJECT_ADD` | `Icons.Outlined.CreateNewFolder` | 创建项目 |
| `PROJECT_DELETE` | `Icons.Outlined.DeleteForever` | 删除项目 |
| `PROJECT_EDIT` | `Icons.Outlined.Settings` | 项目设置 |
| `EYE` | `Icons.Outlined.Visibility` | 查看文件 |

颜色：使用 `MaterialTheme.colorScheme.tertiary`（完成态）或渐变动画（加载态）。
尺寸：20dp × 20dp。

---

## 9. 实施计划

### Phase 1: 数据模型 + 注册表（优先）
- [ ] 创建 `domain/model/ToolDisplayModels.kt`
- [ ] 创建 `util/ToolDisplayRegistry.kt`（工具名 → ToolDisplayInfo 映射）
- [ ] 添加所有三语言字符串（`strings.xml` × 3）

### Phase 2: 结果解析
- [ ] 创建 `util/ToolResultParser.kt`
- [ ] 实现 13 个工具的 `parse*()` 方法
- [ ] 单元测试：每个工具的 JSON 解析

### Phase 3: UI 组件
- [ ] 创建 `ToolExecutionCard.kt` 组件
- [ ] 实现加载态（Loading）、完成态（Success）、错误态（Error）
- [ ] 实现展开/折叠交互
- [ ] 在 `ChatBubble.kt` 中集成新组件

### Phase 4: 消息合并
- [ ] 在 `ChatTab.kt` 中实现 `prepareDisplayItems()`
- [ ] 实现 ASSISTANT(toolCalls) + TOOL 消息的合并逻辑
- [ ] 处理边界情况（中断的生成、部分失败等）
- [ ] 回归测试

### Phase 5: 优化
- [ ] 动画打磨（加载 → 完成过渡）
- [ ] 长文件内容的虚拟滚动（如需要）
- [ ] 搜索结果的 Markdown 渲染
- [ ] 性能优化（避免大列表重解析）

---

## 10. 边界情况处理

| 场景 | 处理方式 |
|------|---------|
| **Tool Call 执行失败** | 红色 `errorContainer` 卡片，显示错误摘要 |
| **Tool Call 执行中用户取消** | 取消当前加载动画，回到 Idle 状态 |
| **ASSISTANT 消息同时有 text + toolCalls** | 先显示 text 气泡，再显示 ToolExecutionCard |
| **单条 ASSISTANT 包含多个 toolCalls** | 每个 toolCall 独立一张卡片 |
| **Tool Call 结果被截断（JSON 不完整）** | 检测 JSONException，显示为 Error 卡片 |
| **只读工具（无需展开内容）** | 不显示展开箭头 |
| **展开内容过长（>500行）** | 限制展开区域最大高度 300dp，超出滚动 |
| **空会话（无消息）** | 不触发合并逻辑 |

---

## 11. 附录

### A. 现有相关文件路径

```
app/src/main/java/com/universe_st/quickwriter/
├── domain/model/
│   ├── ChatModels.kt          # ChatMessage, ToolCall, SessionState
│   └── ToolDisplayModels.kt   # [新增] ToolExecutionResult, ToolDisplayInfo
├── util/
│   ├── ToolResultParser.kt    # [新增] 工具结果解析
│   └── ToolDisplayRegistry.kt # [新增] 工具展示信息注册表
├── presentation/ui/components/
│   ├── ChatBubble.kt          # [修改] 重构为使用 ToolExecutionCard
│   └── ToolExecutionCard.kt   # [新增] 统一工具执行卡片
└── presentation/ui/screens/
    └── ChatTab.kt             # [修改] 消息预处理 + DisplayItem
```

### B. 参考文档

- `docs/implementation/14-ai-chat-system.md` — AI 对话系统实现文档
- `docs/implementation/11-internationalization.md` — 国际化规范（UiText / 三语言）

---

**文档版本**: 1.0  
**最后更新**: 2026-05-01  
**状态**: 设计阶段，待评审
