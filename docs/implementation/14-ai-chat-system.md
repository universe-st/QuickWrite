# AI 对话系统 (AI Chat System)

## 功能概述

基于 Android Foreground Service 的 AI 对话系统，支持多会话并行、流式 SSE 响应、Function Calling (13个工具操作项目文件系统)、操作回溯。所有对话逻辑在 Service 中运行，UI 解绑/重建不中断对话。

## 关键文件

### 数据层
| 文件 | 路径 | 用途 |
|------|------|------|
| AiSessionEntity | `data/local/entity/AiSessionEntity.kt` | ai_sessions 表 — 会话元数据 (34行) |
| AiMessageEntity | `data/local/entity/AiMessageEntity.kt` | ai_messages 表 — 消息记录 (40行) |
| AiOperationEntity | `data/local/entity/AiOperationEntity.kt` | ai_operations 表 — AI 操作回溯 (40行) |
| AiSessionDao | `data/local/dao/AiSessionDao.kt` | 会话 CRUD (39行) |
| AiMessageDao | `data/local/dao/AiMessageDao.kt` | 消息 CRUD + 静默过滤 (40行) |
| AiOperationDao | `data/local/dao/AiOperationDao.kt` | 操作记录 CRUD (21行) |
| Migrations | `data/local/database/Migrations.kt` | v1→v2 数据库迁移 (88行) |
| AiConversationRepository | `data/repository/AiConversationRepository.kt` | 会话+消息持久化封装 + Entity↔Domain 转换 (97行) |

### 网络层
| 文件 | 路径 | 用途 |
|------|------|------|
| AiModels (DTO) | `data/remote/dto/AiModels.kt` | API 请求/响应/流式 DTO (88行) |
| AiApiService | `data/remote/AiApiService.kt` | Retrofit API 接口 (流式+非流式) (28行) |
| AiApiClient | `data/remote/AiApiClient.kt` | OkHttp + Retrofit 实例工厂 (27行) |
| AiServiceRepository | `data/repository/AiServiceRepository.kt` | API 调用封装 + Service 缓存 (55行) |

### Service 层
| 文件 | 路径 | 用途 |
|------|------|------|
| AIChatService | `data/remote/AIChatService.kt` | Foreground Service 入口 + 通知管理 (211行) |
| IChatService | `data/remote/IChatService.kt` | Binder 暴露的服务接口 (43行) |
| SessionManager | `data/remote/SessionManager.kt` | 会话生命周期管理 + 系统提示词构建 + 上下文维护 (284行) |
| ApiDispatcher | `data/remote/ApiDispatcher.kt` | API 调度 + 流式消费 + 消息持久化 + 自动标题 + Tool Call 循环 (302行) |
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
| ChatModels | `domain/model/ChatModels.kt` | ChatMessage / MessageRole / SessionState / SessionSummary / SessionContext / ToolCall (60行) |
| ChatTool | `domain/model/ChatTool.kt` | ChatTool 接口 / ToolDefinition / ToolContext (30行) |
| AiOperation | `domain/model/AiOperation.kt` | AiOperation 抽象基类 + 8 种子类 + OperationType 枚举 + HashUtil (276行) |

### 工具类
| 文件 | 路径 | 用途 |
|------|------|------|
| StreamParser | `util/StreamParser.kt` | SSE 流式响应解析 (87行) |
| TokenEstimator | `util/TokenEstimator.kt` | 字符数/4 近似 token 估算 (12行) |

## 设计架构

```
┌──────────────────────────────────────────────┐
│  UI Layer (Compose)                          │
│  ChatTab → AiChatViewModel → Service 绑定     │
│  仅负责：消息展示、输入交互、会话切换            │
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
| `performSendMessage(sessionId, content, attachedFiles, toolRound)` | 核心消息发送 + Tool Call 循环（递归） |
| `processStreamResponse(...)` | 流式响应消费 → 文本/ToolCall/错误分流 |
| `handleToolCalls(...)` | 构建 ToolCall → 逐个执行 → 结果写回上下文 → 递归再请求 |
| `persistAssistantMessage(...)` | 持久化 AI 回复到 ai_messages |
| `triggerAutoTitleIfNeeded(...)` | 首次用户消息后静默生成标题 |

### ToolExecutor

| 方法 | 说明 |
|------|------|
| `registerTool(tool)` / `registerTools(list)` | 注册工具到内部 Map |
| `getToolDefinitions()` | 返回 OpenAI 格式的 tools 数组 |
| `executeToolCall(toolCallId, functionName, arguments, projectId, sessionId)` | 执行单个工具调用 |
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

## 已知问题/技术债务

1. **API Key 仍为明文存储** — 需求要求加密（EncryptedSharedPreferences），当前未实现
2. **Token 估算粗糙** — 使用字符数/4 近似，未使用 tiktoken 等精确方案
3. **备份 ZIP 创建未实现** — `BackupManager.createProjectBackup()` 有容量检查但未实际调用 `zipProjectToFile()`
4. **StreamParser 对 tool_calls finish_reason 检测依赖 Done chunk** — 未显式检测 `finish_reason == "tool_calls"`，改为检测 `toolCallAccumulators.isNotEmpty() && fullContent.isEmpty()`
5. **并行会话 API 请求使用同一 OkHttp Dispatcher** — 默认 max 5 并发，未为不同 provider 创建独立 Dispatcher
6. **Service 通知图标使用 ic_launcher_foreground** — 应使用专用通知图标

---

**文档版本**: 1.0  
**最后更新**: 2026-04-29  
**状态**: 完成（阶段一～三已实现，阶段四 UI 待开发）
