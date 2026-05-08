# AI 对话系统 (AI Chat System)

## 功能概述

基于 Android Foreground Service 的 AI 对话系统，支持多会话并行、流式 SSE 响应、Function Calling (13个工具操作项目文件系统)、操作回溯。所有对话逻辑在 Service 中运行，UI 解绑/重建不中断对话。

## 关键文件

### UI 层 (新增)
| 文件 | 路径 | 用途 |
|------|------|------|
| AiChatViewModel | `presentation/viewmodel/AiChatViewModel.kt` | 对话 ViewModel — Service 绑定、会话管理、消息监听、模型配置检查 (~230行) |
| ChatTab | `presentation/ui/screens/ChatTab.kt` | 对话 Tab 主界面 — 会话列表、消息列表、输入区域 (~590行) |
| ChatBubble | `presentation/ui/components/ChatBubble.kt` | 消息气泡组件 — 用户/AI/ToolCall/系统消息、复制/重试/删除 (~375行) |

### 数据层
| 文件 | 路径 | 用途 |
|------|------|------|
| AiSessionEntity | `data/local/entity/AiSessionEntity.kt` | ai_sessions 表 — 会话元数据 (34行) |
| AiMessageEntity | `data/local/entity/AiMessageEntity.kt` | ai_messages 表 — 消息记录 (42行) |
| AiOperationEntity | `data/local/entity/AiOperationEntity.kt` | ai_operations 表 — AI 操作回溯 (40行) |
| AiSessionDao | `data/local/dao/AiSessionDao.kt` | 会话 CRUD (39行) |
| AiMessageDao | `data/local/dao/AiMessageDao.kt` | 消息 CRUD + 静默过滤 (40行) |
| AiOperationDao | `data/local/dao/AiOperationDao.kt` | 操作记录 CRUD (21行) |
| Migrations | `data/local/database/Migrations.kt` | v1→v2→v3 数据库迁移 (94行) |
| AiConversationRepository | `data/repository/AiConversationRepository.kt` | 会话+消息持久化封装 + Entity↔Domain 转换 (97行) |

### 网络层
| 文件 | 路径 | 用途 |
|------|------|------|
| AiModels (DTO) | `data/remote/dto/AiModels.kt` | API 请求/响应/流式 DTO (93行) |
| AiApiService | `data/remote/AiApiService.kt` | Retrofit API 接口 (流式+非流式) (28行) |
| AiApiClient | `data/remote/AiApiClient.kt` | OkHttp + Retrofit 实例工厂 (27行) |
| AiServiceRepository | `data/repository/AiServiceRepository.kt` | API 调用封装 + Service 缓存 (55行) |

### Service 层
| 文件 | 路径 | 用途 |
|------|------|------|
| AIChatService | `data/remote/AIChatService.kt` | Foreground Service 入口 + 通知管理 (211行) |
| IChatService | `data/remote/IChatService.kt` | Binder 暴露的服务接口 (43行) |
| SessionManager | `data/remote/SessionManager.kt` | 会话生命周期管理 + 系统提示词构建（委托 PromptManager）+ 上下文维护 (~372行) |
| ApiDispatcher | `data/remote/ApiDispatcher.kt` | API 调度 + 流式消费 + 消息持久化 + 自动标题（委托 PromptManager）+ Tool Call 循环 (~582行) |
| ToolExecutor | `data/remote/ToolExecutor.kt` | 工具注册/分发/执行 + 修改前备份 + 操作记录 (315行) |
| BackupManager | `data/remote/BackupManager.kt` | 备份存储 + FIFO 清理 (5GB上限) (86行) |
| ToolRegistry | `data/remote/ToolRegistry.kt` | 13 个工具统一注册清单 (32行) |
| StateFlowWrapper | `data/remote/StateFlowWrapper.kt` | Binder StateFlow 包装 (6行) |

### 工具层 (13 Tools)
| 文件 | 工具 | 类型 |
|------|------|------|
| `data/remote/tools/GetProjectListTool.kt` | T1. get_project_list | 只读 |
| `data/remote/tools/GetProjectInfoTool.kt` | T2. get_project_info | 只读 |
| `data/remote/tools/GetFolderStructureTool.kt` | T3. get_folder_structure | 只读 |
| `data/remote/tools/ViewFileTool.kt` | T4. view_file | 只读 |
| `data/remote/tools/EditFileTool.kt` | T5. edit_file | 修改 |
| `data/remote/tools/DeleteFileTool.kt` | T6. delete_file | 修改 |
| `data/remote/tools/CreateFileTool.kt` | T7. create_file | 修改 |
| `data/remote/tools/MoveFileTool.kt` | T8. move_file | 修改 |
| `data/remote/tools/CopyFileTool.kt` | T9. copy_file | 修改 |
| `data/remote/tools/SearchInProjectTool.kt` | T10. search_in_project | 只读 |
| `data/remote/tools/UpdateProjectInfoTool.kt` | T11. update_project_info | 修改 |
| `data/remote/tools/CreateProjectTool.kt` | T12. create_project | 修改 |
| `data/remote/tools/DeleteProjectTool.kt` | T13. delete_project | 修改 |

### 领域模型
| 文件 | 路径 | 用途 |
|------|------|------|
| ChatModels | `domain/model/ChatModels.kt` | ChatMessage / MessageRole / SessionState / SessionSummary / SessionContext / ToolCall (55行) |
| ChatTool | `domain/model/ChatTool.kt` | ChatTool 接口 / ToolDefinition / ToolContext (30行) |
| AiOperation | `domain/model/AiOperation.kt` | AiOperation 抽象基类 + 8 种子类 + OperationType 枚举 + HashUtil (276行) |

### 工具类
| 文件 | 路径 | 用途 |
|------|------|------|
| StreamParser | `util/StreamParser.kt` | SSE 流式响应解析，兼容多格式 + reasoning_content (~123行) |
| TokenEstimator | `util/TokenEstimator.kt` | 字符数/4 近似 token 估算 (12行) |
| PromptManager | `util/PromptManager.kt` | 提示词模板加载与变量替换 (~54行) |

## 设计架构

```
┌──────────────────────────────────────────────┐
│  UI Layer (Compose) — 已实现 ✅               │
│  ChatTab → AiChatViewModel → Service 绑定     │
│  负责：消息展示、输入交互、会话切换、侧栏管理     │
│  ┌────────────────────────────────────────┐   │
│  │ ChatBubble                             │   │
│  │  UserBubble (右对齐, PrimaryContainer)   │   │
│  │  AssistantBubble (左对齐, Markdown渲染)  │   │
│  │  ToolCallBubble (可展开/折叠工具调用)     │   │
│  │  TypingIndicator (动画点指示器)          │   │
│  │  工具栏: 复制/重试/删除                 │   │
│  └────────────────────────────────────────┘   │
├──────────────────────────────────────────────┤
│  AIChatService (Foreground Service)          │
│  ┌──────────────┐  ┌───────────────────────┐ │
│  │SessionManager│  │    ApiDispatcher      │ │
│  │ 会话生命周期  │  │ 流式API调度/持久化     │ │
│  │ 上下文维护    │  │ 自动标题/FnCall循环    │ │
│  │ 空闲回收     │  └───────────┬───────────┘ │
│  └──────────────┘              │             │
│                        ┌───────▼───────────┐ │
│                        │   ToolExecutor    │ │
│                        │ 13个工具注册/执行   │ │
│                        │ BackupManager备份  │ │
│                        │ Operation回溯      │ │
│                        └───────────────────┘ │
├──────────────────────────────────────────────┤
│  Data Layer                                  │
│  Retrofit → AiApiService                     │
│  Room → ai_sessions/ai_messages/ai_operations│
└──────────────────────────────────────────────┘
```

### DI 桥接

Service 由系统创建（无法通过构造函数注入），通过 `AppServiceContainer` 单例桥接：

```
AppContainer.init
  → AppServiceContainer.{dao, repository, useCase} = ...
  
AIChatService.onCreate
  → SessionManager(AppServiceContainer.*)
  → ToolExecutor(AppServiceContainer.*, backupManager)
  → ApiDispatcher(AppServiceContainer.*, sessionManager, toolExecutor)
```

## 数据流

### 消息发送完整流程

```
sendMessage(sessionId, content)
  │
  ├─ 1. ApiDispatcher.performSendMessage(toolRound)
  │   ├─ 获取 SessionContext（系统提示词 + 历史消息）
  │   ├─ 持久化 user 消息到 ai_messages 表
  │   ├─ 追加到内存上下文
  │   ├─ 构建 ChatCompletionRequest
  │   │   ├─ 含 tools 定义（从 ToolExecutor.getToolDefinitions()）
  │   │   └─ 含截断策略（反向遍历，保留系统提示词 + 6000 token 内消息）
  │   └─ 发起流式 API 调用
  │
  ├─ 2. processStreamResponse()
  │   ├─ BufferedReader → 逐行读取 SSE
  │   ├─ StreamParser.parseLine()
  │   │   ├─ Content → SessionState.Generating(partialContent)
  │   │   ├─ ToolCallBegin/Args → ToolCallAccumulator 收集
  │   │   └─ Done → usage tokens
  │   └─ 异常保护: 部分内容先持久化再抛异常
  │
  ├─ 3a. 普通文本结束
  │   ├─ persistAssistantMessage() → ai_messages
  │   ├─ triggerAutoTitleIfNeeded() → 首次用户消息后静默生成标题
  │   └─ SessionState.Idle
  │
  └─ 3b. Tool Call 被调用
      ├─ 持久化 assistant 消息（含 tool_calls JSON）
      ├─ handleToolCalls():
      │   ├─ 逐个工具执行:
      │   │   ├─ ToolExecutor.prepareBackup() → BackupManager 备份文件
      │   │   ├─ ToolExecutor.executeToolCall() → 执行 → 返回结果
      │   │   ├─ ToolExecutor.recordOperation() → ai_operations 表
      │   │   └─ 持久化 tool 结果消息
      │   └─ performSendMessage(toolRound + 1) ← 循环回调
      └─ 10轮强制终止 → SessionState.Error
```

### 自动标题流程

```
首次用户消息 → AI 回复完成后
  │
  ├─ 检测 ai_messages 中非静默 user 消息仅 1 条
  ├─ 构建静默请求（非流式 API）
  │   └─ system: "You are a title generator..."
  │   └─ user: "请为以下对话生成不超过10字的标题..."
  ├─ 成功 → SessionManager.renameSession()
  └─ 失败 → 回退: 截取首条用户消息前30字符
```

## 核心类/函数

### SessionManager

| 方法 | 说明 |
|------|------|
| `createSession(projectId, systemPrompt?, modelConfigId?)` | 创建会话 → 缓存 + DB 异步写入 |
| `createSessionWithProjectInfo(...)` | 使用项目信息自动构建系统提示词 |
| `getSessionContext(sessionId)` | 获取内存中的会话上下文 |
| `loadSessionContext(sessionId)` | 从 DB 加载 + 解析消息 → 恢复上下文 |
| `appendMessageToContext(sessionId, msg)` | 追加消息到内存上下文 |
| `scheduleIdleRecycle()` | 5分钟空闲后自动清除（可取消） |
| `buildSystemPrompt(title, author, genre, storagePath)` | 静态方法 — 构建系统提示词 |

### ApiDispatcher

| 方法 | 说明 |
|------|------|
| `performSendMessage(sessionId, content, attachedFiles, toolRound)` | 核心消息发送 + Tool Call 循环（递归）。对 `create_file`/`edit_file` 工具描述动态追加字符限制提示（`maxTokens/2.5`） |
| `processStreamResponse(...)` | 流式响应消费 → 文本/ToolCall/错误分流 |
| `handleToolCalls(...)` | 构建 ToolCall → 逐个执行 → 结果写回上下文 → 递归再请求 |
| `persistAssistantMessage(...)` | 持久化 AI 回复到 ai_messages |
| `triggerAutoTitleIfNeeded(...)` | 首次用户消息后静默生成标题 |

### ToolExecutor

| 方法 | 说明 |
|------|------|
| `registerTool(tool)` / `registerTools(list)` | 注册工具到内部 Map |
| `getToolDefinitions()` | 返回 OpenAI 格式的 tools 数组 |
| `executeToolCall(toolCallId, functionName, arguments, projectId, sessionId)` | 执行单个工具调用。`argumentsJson` 解析失败（JSONException）时，返回截断提示错误，引导用户增大 max_tokens |
| `rollbackOperation(operationId, projectId)` | 回溯操作（检查 canRollback → 执行 rollback → 清理记录） |
| `prepareBackup(...)` | 修改前备份文件到 ai_backups/ |
| `recordOperation(...)` | 写入 AiOperationEntity 到 Room |

### BackupManager

| 方法 | 说明 |
|------|------|
| `ensureCapacity(requiredSize)` | 检查 + FIFO 清理确保空间（5GB 上限） |
| `createOperationBackup(operationId, sourceFile)` | 创建文件备份 |
| `clearAll()` | 清空全部备份 |
| `getTotalBackupSize()` | 计算备份总大小 |

## 关键实现细节

### 1. 双向通信机制
- Service 与 ViewModel 通过 **Binder + StateFlow** 通信
- `IChatService` 接口定义所有 Binder 暴露方法
- `StateFlowWrapper<T>` 包装 StateFlow 以供 Binder 传递引用
- 所有非 suspend 方法内部通过 `launch { }` 异步执行 DAO 操作

### 2. Tool Call 循环
- 流式解析中通过 `ToolCallAccumulator` 跨 chunk 收集 tool call 参数
- `performSendMessage` 递归调用实现最多 10 轮 Tool Call
- 每轮结束后将 assistant 消息（含 tool_calls）和 tool 结果消息追加到上下文
- 用户取消生成时立即中断循环

### 3. Token 截断策略
- 使用 `TokenEstimator`（字符数/4）估算 token 数
- 构建 API 请求时从末尾反向遍历消息列表
- 超过 `MAX_CONTEXT_TOKENS`(6000) 时截断最早的非系统消息
- 系统提示词始终保留

### 4. 操作回溯
- 修改性工具（7种）执行前自动备份 → 执行后记录 `AiOperationEntity`
- 每种操作类型有独立的 `canRollback()` 判断逻辑（哈希对比/备份存在/文件状态）
- `rollback()` 执行回溯恢复 → 成功后清理备份和数据库记录
- 备份采用 FIFO 自动清理，总上限 5GB

### 5. 安全性
- 所有文件操作路径通过 `FileManager.isPathSafe()` 验证
- 项目删除需提供 `confirmTitle` 双重确认
- 核心目录（正文/、设定/、时间线/、记录/、配置/）受到保护

### 6. 自动回收
- `SessionManager.scheduleIdleRecycle()` — 5 分钟空闲后自动清除内存
- `switchToSession()` 或新消息到达时取消回收计时
- Service.onDestroy() 时清理所有会话

### 7. 消息持久化时机
| 事件 | 行为 |
|------|------|
| 用户消息 | **立即**写入 ai_messages |
| AI 流式中间 token | **仅内存**，不写入 DB |
| AI 回复完成 | **一次性**写入 ai_messages |
| Tool Call 消息 | **立即**写入（assistant + tool result） |
| 标题生成 | 更新 ai_sessions.title |

### 8. 流式解析容错

**格式兼容性** (StreamParser):
- `data:` 前缀：同时接受 `data:` 和 `data: `（冒号后空格可选）
- 内容提取路径（优先级递减）：
  1. `choices[0].delta.content` — OpenAI/DeeepSeek 标准
  2. `choices[0].content` — 部分平台直接放 choice 层
  3. `choices[0].text` — 旧格式
  4. 顶层 `content` — 极简格式
- **JsonNull 安全处理**: 所有 `get("key")` 调用通过 `optString(key)` 扩展函数，先检查 `isJsonNull` 再调用 `asString()`，避免 `UnsupportedOperationException`

**API 请求上下文修复** (ApiDispatcher.performSendMessage):
- `context` 变量必须在用户消息追加到 `SessionContext` 之后再读取，否则 `buildMessagesForApi()` 只含 system 提示词、不含用户消息
- 当前实现：通过 `sessionManager.getSessionContext(sessionId)` 重新获取上下文

**HTTP 错误响应处理** (ApiDispatcher):
- 新增 `response.isSuccessful` 检查，失败时读取 `errorBody()` 内容显示具体错误
- 流结束后若 `fullContent` 为空且无 tool calls，设置错误状态（非静默回 Idle）

**流式渲染防闪烁** (ChatTab / ChatBubble):
- 生成中的气泡已合并到 `displayMessages` 列表中（`id = Long.MAX_VALUE` 为稳定 key），不再是 LazyColumn 中独立的 `item {}`，避免消息更新时重建
- `LaunchedEffect` 仅依赖 `messages.size` 触发滚动，不再随每个 token 变化自动滚动
- 流结束时先设置 `SessionState.Idle` 再持久化 AI 回复（避免生成中气泡与持久化消息双显示）
- Markdown 渲染使用 `com.github.jeziellago:compose-markdown:0.7.2`

### 9. 会话创建模型配置注入

**createSession 流程** (AiChatViewModel):
- 当 `modelConfigId` 参数为 `null` 时，通过 `AiModelConfigRepository.getDefaultConfig()` 查询默认配置 ID → 传递给 Service 创建会话
- 若默认配置不存在则回退到 `getAllConfigs().first()`（首个可用配置）
- ApiDispatcher 中 `performSendMessage` 增加二级容错：`getConfigById` 失败时回落 `getDefaultConfig()`

**必需权限**:
- `INTERNET` — HTTP API 调用必须声明，初始版本中缺失导致 `SecurityException`
- `ACCESS_NETWORK_STATE` — 网络状态检查

### 10. Tool Call JSON 截断检测

`ToolExecutor.executeToolCall()` 中解析 `argumentsJson` 时，若因 `maxTokens` 过低导致 JSON 不完整（`JSONException`），返回明确错误提示，引导用户增大 `maxTokens`。

截断原因：`create_file` 的 `content` 或 `edit_file` 的 `newContent` 参数直接嵌入 JSON，长文本可能超出 `maxTokens` 限制，API 端截断流式响应导致 JSON 不完整。

### 11. 动态工具描述

`create_file` 和 `edit_file` 的工具描述在 `ApiDispatcher.performSendMessage()` 中动态追加字符限制提示，根据当前 `modelConfig.maxTokens` 计算：`maxTokens / 2.5 ≈ 单次调用建议上限字符数`。

工具自身 `ToolDefinition.description` 不含硬编码数字，动态部分由 ApiDispatcher 构建 API 请求时追加。

### 12. maxTokens 默认值

`AiModelConfigEntity`、`UserSettingsRepository`、`AiConfigFormData` 等全链路默认值从 `2000` 提升至 `50000`（9 处）。

### 13. 提示词外部化

所有 AI 系统提示词从硬编码字符串迁移至 `assets/prompts/*.md` 文件：

| 模板文件 | 用途 | 变量 |
|---------|------|------|
| `default_assistant.md` | `createSession()` 默认提示词 | 无 |
| `novel_writing_assistant.md` | `createSessionWithProjectInfo()` 项目提示词 | `{{title}}` `{{author}}` `{{genre}}` `{{storagePath}}` |
| `title_generator.md` | `triggerAutoTitleIfNeeded()` 标题生成器 | 无 |

`PromptManager` 在 `AIChatService` 创建时加载全部模板到内存，通过 `{{变量}}` 语法替换。修改提示词只需编辑 `.md` 文件。

## UI 层实现

### AiChatViewModel

| 方法 | 说明 |
|------|------|
| `bindToService()` | init 块中绑定 AIChatService，启动 Foreground Service |
| `loadSessions(projectId)` | 从 Room Flow 加载项目会话列表，自动选中首个会话 |
| `selectSession(sessionId)` | 切换会话 → 观察消息 Flow + 会话状态 Flow |
| `createSession(projectId, systemPrompt?, modelConfigId?)` | 通过 Service 创建新会话 |
| `sendMessage()` | 发送输入框文本到当前会话（先检查模型配置状态，无配置时不清空输入） |
| `stopGeneration()` | 停止当前会话的生成 |
| `retryLastMessage()` | 重试最后一条消息 |
| `deleteMessage(index)` | 删除指定消息索引 |
| `deleteSession(sessionId)` | 删除整个会话 |

**依赖**: `AiConversationRepository` + `AiModelConfigRepository`（用于检查 `hasAnyConfig()` 状态）

### 模型配置前置检查

- `init` 和 `loadSessions()` 中调用 `hasAnyConfig()` 获取 `hasModelConfig` 状态
- `sendMessage()` 中若 `!hasModelConfig`，直接 return，不清空输入框文本
- ChatTab 中检测 `!hasModelConfig` 时展示 `NoModelConfigState` 引导页面，含"配置 AI 模型"按钮

### 消息观察机制

- **消息列表**: `AiConversationRepository.getVisibleMessages(sessionId)` Flow — Service 写入 Room → DAO Flow 自动推送到 UI
- **会话状态**: `IChatService.observeSessionState(sessionId).asStateFlow()` — 通过修改后的 StateFlowWrapper 实时收集 Generating/Idle/Error 状态
- **会话列表**: `AiConversationRepository.getSessionsByProject(projectId)` Flow — 按项目过滤的会话列表

### ChatTab 组件结构

```
ChatTab(projectId, onNavigateToAiConfig?)
├── [无模型配置] → NoModelConfigState（含"配置 AI 模型"按钮 → 导航到 Settings > AI Config）
├── [无会话] → ChatEmptyState（"新建对话"按钮）
├── Row
│   ├── SessionSidebar (AnimatedVisibility, 240dp宽, 支持左滑关闭)
│   │   ├── Header (标题 + 新建 + 关闭按钮)
│   │   └── LazyColumn of SessionListItem (长按删除)
│   ├── VerticalDivider
│   └── ChatContentArea (Modifier.weight(1f))
│       ├── ChatTopBar (会话标题 + 侧栏切换)
│       ├── ErrorBanner (SessionState.Error 时显示)
│       ├── MessageList (LazyColumn, 自动滚底)
│       │   ├── MessageBubble (按角色分发)
│       │   └── TypingIndicator (生成中)
│       └── ChatInputArea
│           ├── OutlinedTextField (圆形, 多行)
│           └── FilledIconButton (Send / Stop)
├── DeleteSessionDialog (确认)
└── DeleteMessageDialog (确认)
```

**会话侧栏交互**:
- **点击内容区关闭**: 侧栏打开时，`ChatContentArea` 上方覆盖透明可点击层，点击对话内容/输入框等区域自动关闭侧栏
- **左滑关闭**: `SessionSidebar` 使用 `pointerInput` + `detectHorizontalDragGestures` 检测左滑手势（阈值 120px），触发 `viewModel.showSidebar = false`
- **长按删除**: `SessionListItem` 使用 `combinedClickable(onLongClick=...)` 触发删除确认对话框
- **关闭按钮**: 侧栏顶部 Close 图标按钮，功能同上

### 数据流向

```
用户输入 → sendMessage()
  → IChatService.sendMessage(sessionId, text)
    → SessionManager 创建 USER 消息 → Room (ai_messages)
    → ApiDispatcher 调用 LLM API (流式 SSE)
    → 每个 chunk 更新 SessionState.Generating(partialContent)
      → StateFlowWrapper.asStateFlow() → AiChatViewModel._sessionState
      → UI 显示实时流式文本
    → 完成后: 持久化 ASSISTANT 消息 → Room
      → DAO Flow 推送 → AiChatViewModel._messages
      → LazyColumn 自动滚底显示完整回复
```

### 依赖变更

| 变更 | 文件 | 说明 |
|------|------|------|
| StateFlowWrapper 增加 `asStateFlow()` | `data/remote/StateFlowWrapper.kt` | 支持 Binder StateFlow 的响应式收集 |
| AiChatViewModel 依赖 `AiModelConfigRepository` | `AiChatViewModel.kt` | 用于检查 `hasAnyConfig()` 前置拦截 |
| ChatTab 新增 `NoModelConfigState` 组件 | `ChatTab.kt` | 未配置模型时的引导页面 |
| ChatTab 新增 `onNavigateToAiConfig` 回调 | `ChatTab.kt` / `WritingScreen.kt` / `MainScreen.kt` | 从对话界面一键导航到 AI 配置 |
| SettingsScreen 新增 `initialSubScreen` 参数 | `SettingsScreen.kt` | 支持外部指定初始子页面 |
| 新增 Compose Markdown 渲染库 | `gradle/libs.versions.toml` | `com.github.jeziellago:compose-markdown:0.7.2` |
| 新增 chat 引导字符串 (chat_no_config_*) | `res/values*/strings.xml` | 三语言 (EN/zh-CN/zh-TW) |

## 已知问题/技术债务

1. **API Key 仍为明文存储** — 需求要求加密（EncryptedSharedPreferences），当前未实现
2. **Token 估算粗糙** — 使用字符数/4 近似，未使用 tiktoken 等精确方案
3. **备份 ZIP 创建未实现** — `BackupManager.createProjectBackup()` 有容量检查但未实际调用 `zipProjectToFile()`
4. **StreamParser 对 tool_calls finish_reason 检测依赖 Done chunk** — 未显式检测 `finish_reason == "tool_calls"`，改为检测 `toolCallAccumulators.isNotEmpty() && fullContent.isEmpty()`
5. **并行会话 API 请求使用同一 OkHttp Dispatcher** — 默认 max 5 并发，未为不同 provider 创建独立 Dispatcher
6. **Service 通知图标使用 ic_launcher_foreground** — 应使用专用通知图标

## 已修复问题 (2026-04-30)

| 问题 | 修复方式 | 涉及文件 |
|------|---------|---------|
| 新建会话 modelConfigId = 0 导致 API key 查找失败 | createSession 先查默认配置 ID | AiChatViewModel.kt |
| performSendMessage 上下文变量捕获过早 | 用户消息追加后重新读取 context | ApiDispatcher.kt |
| HTTP 非 2xx 响应未检测、错误不明确 | 添加 `isSuccessful` + `errorBody()` 检查 | ApiDispatcher.kt |
| JsonNull 导致 `UnsupportedOperationException` | `optString()` 安全解析全部 8 处 | StreamParser.kt |
| SSE `data:` 前缀变体不兼容 | 接受 `data:` 和 `data:`（空格可选） | StreamParser.kt |
| 内容提取仅支持单一路径 | 新增 4 种 fallback 路径 | StreamParser.kt |
| 生成中气泡 LazyColumn 独立 item 导致闪烁 | 合并为 `displayMessages` + 稳定 key | ChatTab.kt |
| Markdown 组件重渲染闪烁 | 更新为 `jeziellago/compose-markdown` | ChatBubble.kt, libs.versions.toml |
| `INTERNET` 权限缺失 | 添加 `INTERNET` + `ACCESS_NETWORK_STATE` | AndroidManifest.xml |
| 流结束生成中气泡与持久化消息双显示 | 先设 Idle 再持久化 | ApiDispatcher.kt |
| DeepSeek thinking mode reasoning_content 未回传导致 API 400 错误 | 流式解析 `reasoning_content` → 持久化到 DB → 下次请求回传给 API | AiModels.kt, ChatModels.kt, StreamParser.kt, ApiDispatcher.kt, AiMessageEntity.kt, Migrations.kt, SessionManager.kt, AiConversationRepository.kt |
| 流式 Tool Call 参数丢失导致 `view_file` 循环缺少 `relativePath` | arguments chunk 不再依赖 `id`，按 `index` 累积到对应 ToolCall，并保留首 chunk 中的初始 arguments | StreamParser.kt, ApiDispatcher.kt, StreamParserTest.kt |

## 已修复问题 (2026-05-01)

| 问题 | 修复方式 | 涉及文件 |
|------|---------|---------|
| Tool Call JSON 因 max_tokens 过低被截断 | `executeToolCall()` 中捕获 `JSONException`，返回截断提示引导用户增大 max_tokens | ToolExecutor.kt |
| create_file/edit_file 描述含硬编码字符上限 | 工具描述改为动态追加 `maxTokens/2.5` 字符限制提示 | ApiDispatcher.kt, CreateFileTool.kt, EditFileTool.kt |
| maxTokens 默认值 2000 不足 | 全链路默认值提升至 50000（9 处） | AiModelConfigEntity.kt, AiModelConfigRepository.kt, SettingsUseCase.kt, SettingsViewModel.kt, UserSettingsRepository.kt, AiModels.kt |
| max_tokens 和 max_tool_rounds 拖曳条输入不便 | 新增 `SettingsIntEditItem` 组件（`OutlinedTextField` + `KeyboardType.Number`），替换两个 `SettingsSliderItem` | SettingsComponents.kt, WritingSettingsScreen.kt, AiConfigScreen.kt |
| 系统提示词硬编码在 Kotlin 中，不便修改 | 3 个提示词抽成 `assets/prompts/*.md` 模板文件，新增 `PromptManager` 加载和变量替换 | assets/prompts/*.md, PromptManager.kt, SessionManager.kt, ApiDispatcher.kt, AIChatService.kt |
| 消息截断导致 TOOL 消息与 ASSISTANT tool_calls 分离，API 返回 400 'tool' must be a response to 'tool_calls' | `buildMessagesForApi()` 新增 `ensureToolCallPairs()` 方法，截断后向后搜索补齐缺失的 ASSISTANT tool_calls 消息 | ApiDispatcher.kt |

## 已修复问题 (2026-05-08)

| 问题 | 修复方式 | 涉及文件 |
|------|---------|---------|
| “滚动到底端”按钮滚动后又跳回顶部 | 按钮触发单次动画滚动到末尾，并在动画期间用全屏遮罩锁定其它交互 | ChatTab.kt |

---

**文档版本**: 1.8  
**最后更新**: 2026-05-08  
**状态**: 完成（含 Tool Call JSON 截断检测、动态工具描述、maxTokens 默认值提升、整数编辑框、提示词外部化、聊天滚动修复）
