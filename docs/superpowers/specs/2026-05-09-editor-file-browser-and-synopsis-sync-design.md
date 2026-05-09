# 编辑器文件浏览 & 简介同步 设计文档

**日期**: 2026-05-09  
**版本**: 1.0  
**状态**: 待实现

---

## 一、概述

本次开发包含两大功能模块：

| 模块 | 描述 |
|------|------|
| **编辑器文件浏览** | 在写作编辑器的左侧面板增加文件夹切换功能，支持浏览和编辑项目中除章节外的文件（设定/时间线/记录/配置） |
| **简介与 info.json 同步** | 将简介从 简介.md 迁移到 info.json，与数据库双向同步，并注入 AI 提示词 |

---

## 二、编辑器左侧面板文件浏览

### 2.1 当前架构

```
EditorContent
├── AnimatedVisibility → ChapterListPanel (220dp, 左侧)
│   ├── 标题行：「章节」+ 「+」按钮
│   ├── LazyColumn → ChapterListItem (每个章节一行)
│   │   ├── 上移/下移按钮
│   │   ├── 标题/卷名/摘要
│   │   └── 删除按钮
│   └── 底部：章节计数
└── MarkorEditor (右侧，剩余空间)
```

### 2.2 改造后架构

```
EditorContent
├── AnimatedVisibility → FileListPanel (220dp, 左侧)
│   ├── 标题行：「章节▼」+ 「···」下拉菜单 + 「+」按钮
│   │   └── DropdownMenu: 章节 | 设定 | 时间线 | 记录 | 配置
│   ├── LazyColumn
│   │   ├── [章节模式] ChapterListItem（上移/下移/删除/标题/卷名/摘要）
│   │   └── [文件模式] FileTreeItemNode（文件夹可折叠/文件可点击/删除）
│   └── 底部：文件计数
└── MarkorEditor (右侧，剩余空间) — 共用
```

### 2.3 数据模型

```kotlin
// 文件树节点
data class FileTreeItem(
    val name: String,            // 文件名或文件夹名
    val relativePath: String,    // 相对于项目 storagePath 的路径
    val absolutePath: String,    // 绝对路径
    val isDirectory: Boolean,
    val lastModified: Long,
    val children: List<FileTreeItem> = emptyList(),
    val size: Long = 0
)

// 文件浏览模式
enum class FileBrowserMode {
    CHAPTERS,    // 章节（正文/）
    SETTINGS,    // 设定/
    TIMELINE,    // 时间线/
    LOGS,        // 记录/
    CONFIG       // 配置/
}
```

### 2.4 行为对照表

| 行为 | 章节模式 (CHAPTERS) | 非章节模式 (SETTINGS/TIMELINE/LOGS/CONFIG) |
|------|-------------------|-------------------------------------------|
| **排序** | `order` 字段升序 | `lastModified` 降序 |
| **子文件夹** | 无 | 可折叠展开/收起（树形结构） |
| **元数据** | YAML Front Matter（title/order/volume/summary） | 无（纯文件） |
| **上移/下移** | 显示 | 隐藏 |
| **删除** | 弹二次确认弹窗 | 弹二次确认弹窗 |
| **长按** | 弹出菜单：修改文件名、修改元数据 | 弹出菜单：修改文件名 |
| **点击** | 加载章节到编辑器（剥离 YAML 后显示） | 加载文件原文到编辑器 |
| **"+"按钮** | 弹出新建章节对话框 | 弹出下拉菜单：添加文件 / 添加文件夹 |
| **保存** | 用 ChapterFileHelper 重组 YAML + 正文写入 | 直接覆盖写源文件 |

### 2.5 UI 组件清单

| 组件 | 类型 | 用途 |
|------|------|------|
| `FileListPanel` | Composable | 改造自 `ChapterListPanel`，根据 mode 显示不同内容 |
| `ChapterListItem` | Composable | 保持不变，章节列表行 |
| `FileTreeItemNode` | Composable | 新增，文件树节点（文件/文件夹行） |
| `FolderModeDropdown` | Composable | 新增，标题旁下拉菜单（章节/设定/时间线/记录/配置） |
| `AddFileMenu` | Composable | 新增，"+"按钮的下拉菜单（添加文件/文件夹） |
| `DeleteConfirmDialog` | Composable | 新增，统一的删除二次确认弹窗 |
| `RenameFileDialog` | Composable | 新增，重命名文件弹窗 |
| `EditChapterMetaDialog` | Composable | 新增，编辑章节元数据弹窗 |
| `NewFileDialog` | Composable | 新增，新建文件弹窗（输入文件名） |
| `NewFolderDialog` | Composable | 新增，新建文件夹弹窗（输入文件夹名） |

### 2.6 文件操作逻辑

#### 点击文件
1. 读取文件内容 → `readFileContent(filePath)`
2. 更新 `editorContent` 为文件内容（不做 YAML 解析）
3. 设置 `currentFilePath`、`fileBrowserMode`
4. Editor 显示内容，允许编辑

#### 保存非章节文件
1. 获取当前 `editorContent`
2. 直接 `writeFileContent(currentFilePath, content)`
3. 刷新文件树

#### 删除文件/文件夹（二次确认弹窗）
1. 长按或点击删除 → 弹出 `DeleteConfirmDialog`
2. 文案："确定删除 [name]？此操作不可撤销。"
3. 确认后：
   - 文件：`File.delete()`
   - 文件夹（非空）：递归删除子文件和子文件夹
4. 如果当前正在编辑该文件 → 清空编辑器状态
5. 刷新文件树

#### 重命名文件（长按菜单）
1. 长按 → 弹出菜单 → 选择"修改文件名"
2. 弹出 `RenameFileDialog`
3. 校验：不能含 `/ \ : * ? " < > |` ，不能与同目录文件重名
4. 执行 `File.renameTo()`
5. 刷新文件树

#### 添加文件/文件夹
1. 点击"+"→ 选择"添加文件"或"添加文件夹"
2. 弹出对应对话框，输入名称
3. 校验：合法文件名，不重名
4. 在当前浏览目录下创建文件/文件夹
5. 刷新文件树

#### 编辑章节元数据（长按菜单）
1. 长按章节 → 弹出菜单 → 选择"修改元数据"
2. 弹出 `EditChapterMetaDialog`，可编辑 title / volume / summary / order
3. 保存时：调用 `ChapterFileHelper.buildChapterContent()` 重建文件
4. 刷新章节列表

---

## 三、info.json 同步 & 提示词注入

### 3.1 当前状态

| 位置 | 包含 description? | 写入时机 |
|------|------------------|---------|
| `ProjectEntity.description` (DB) | ✅ | 创建、编辑时 |
| `info.json` (文件系统) | ❌ | 创建时 |
| `novel_writing_assistant.md` (提示词) | ❌ | — |
| `简介.md` (文件系统) | — | 创建时生成空文件 |

### 3.2 目标状态

| 位置 | 包含 description? | 写入时机 |
|------|------------------|---------|
| `ProjectEntity.description` (DB) | ✅ | 创建、编辑时（不变） |
| `info.json` (文件系统) | ✅ **新增字段** | 创建、编辑时（新增） |
| `novel_writing_assistant.md` | ✅ **新增变量** | 构建系统提示词时注入 |
| `简介.md` | ❌ **不再创建** | — |

### 3.3 info.json 格式

```json
{
  "title": "书名",
  "author": "作者",
  "genre": "类型",
  "description": "小说简介/核心设定",
  "createdTime": "2026-04-28T12:00:00.000Z",
  "version": "1.0"
}
```

### 3.4 变更清单

#### FileManager.kt

| 方法 | 变更 |
|------|------|
| `createInfoJson()` | 增加 `description: String` 参数，写入 JSON |
| `readInfoJson()` → `InfoJsonData` | `InfoJsonData` 增加 `description: String` 字段 |
| `createProjectDirectoryStructure()` | 移除 `createIntroFile()` 调用 |
| `createDirectoryStructureAt()` | 移除 `File(projectDir, "简介.md").createNewFile()` |
| `createIntroFile()` | 删除此方法（或保留但不再调用） |

#### ProjectManagementUseCase.kt

| 方法 | 变更 |
|------|------|
| `createProject()` | `createInfoJson()` 调用增加 `description` 参数 |
| `updateProject()` | **新增**：最后调用 `updateInfoJson()` 同步文件系统 |
| `exportProjectAsZip()` | `createInfoJson()` 调用增加 `description` 参数 |

#### PromptManager.kt

| 方法 | 变更 |
|------|------|
| `getNovelWritingAssistantPrompt()` | 增加 `description: String` 参数 |

#### SessionManager.kt

| 方法 | 变更 |
|------|------|
| `buildSystemPrompt()` | 增加 `description` 参数，传入 `PromptManager` |

#### novel_writing_assistant.md

| 位置 | 变更 |
|------|------|
| "## 项目信息" 区块 | 新增 `- **简介**：{{description}}` |
| "## 项目目录结构" | 移除 `├── 简介.md` 行 |
| 全文 | 删除所有对 简介.md 的引用 |

#### AGENTS.md

| 位置 | 变更 |
|------|------|
| "项目目录结构" | 移除 `├── 简介.md` |
| "info.json 格式" | 增加 `"description": "..."` 字段说明 |

---

## 四、WritingViewModel 变更

### 4.1 新增状态字段

在 `WritingUiState.Success` 中增加：

```kotlin
data class Success(
    // ... 现有字段保持不变 ...
    val fileBrowserMode: FileBrowserMode = FileBrowserMode.CHAPTERS,
    val fileTree: List<FileTreeItem> = emptyList(),
    val expandedFolders: Set<String> = emptySet(),
    val currentFilePath: String? = null,          // null = 章节模式
    val showDropdownMenu: Boolean = false,
    val showAddFileMenu: Boolean = false,
    val showDeleteConfirm: DeleteConfirmData? = null,
    val showRenameDialog: RenameDialogData? = null,
    val showEditMetaDialog: EditMetaDialogData? = null,
    val showNewFileDialog: Boolean = false,
    val showNewFolderDialog: Boolean = false
)
```

### 4.2 新增方法

```kotlin
fun switchBrowseMode(mode: FileBrowserMode)
fun loadFileTree(mode: FileBrowserMode)
fun toggleFolderExpanded(path: String)
fun selectNonChapterFile(file: FileTreeItem)
fun saveCurrentFile()
fun deleteFileOrFolder(item: FileTreeItem)
fun renameFile(oldPath: String, newName: String)
fun createNewFile(parentDir: String, fileName: String)
fun createNewFolder(parentDir: String, folderName: String)
fun requestDeleteConfirm(item: FileTreeItem)
fun dismissDeleteConfirm()
fun requestRename(item: FileTreeItem)
fun requestEditMeta(chapterIndex: Int)
```

### 4.3 目录路径映射

```kotlin
fun FileBrowserMode.toDirPath(storagePath: String): String = when (this) {
    FileBrowserMode.CHAPTERS -> "$storagePath/正文"
    FileBrowserMode.SETTINGS -> "$storagePath/设定"
    FileBrowserMode.TIMELINE -> "$storagePath/时间线"
    FileBrowserMode.LOGS -> "$storagePath/记录"
    FileBrowserMode.CONFIG -> "$storagePath/配置"
}
```

---

## 五、影响范围

### 5.1 修改文件

| 文件 | 变更类型 |
|------|---------|
| `WritingScreen.kt` | 重构（ChapterListPanel → FileListPanel，新增多个弹窗组件） |
| `WritingViewModel.kt` | 扩展（新增文件浏览状态和方法） |
| `FileManager.kt` | 扩展（getFileTree, updateInfoJson, InfoJsonData 增加 description） |
| `ProjectManagementUseCase.kt` | 扩展（updateProject 增加 info.json 同步） |
| `PromptManager.kt` | 扩展（增加 description 参数） |
| `SessionManager.kt` | 扩展（传入 description） |
| `novel_writing_assistant.md` | 修改（增加 description 变量，删除 简介.md 引用） |
| `AGENTS.md` | 修改（更新目录结构和 info.json 描述） |

### 5.2 新增文件

| 文件 | 说明 |
|------|------|
| 无 | 所有新增逻辑在现有文件中实现 |

### 5.3 不修改文件

- `ChapterFileHelper.kt` — 章节格式不变
- `ProjectEntity.kt` — DB 表结构不变（已有 description 字段）
- `ProjectDao.kt` — 不变
- `ProjectRepository.kt` — 不变（已有 description 字段在 update 时携带）
- `ChatTab.kt` — 不变
- `markor-editor` 模块 — 不变

---

## 六、边界条件 & 注意事项

1. **存储路径规则**：所有路径操作必须使用 `project.storagePath`，不得使用 `FileManager.getProjectDirectory()`
2. **非章节文件编码**：统一 UTF-8
3. **空目录处理**：目录为空时显示 string 资源字符串提示
4. **文件夹删除安全**：只删除项目 storagePath 目录下的文件，防止路径遍历攻击
5. **文件名校验**：禁止含 `/ \ : * ? " < > |`，禁止重名
6. **国际化**：所有新增 UI 文字需在三个 `strings.xml` 中添加对应条目
7. **已有 简介.md**：不主动删除已有项目中的 简介.md，仅停止创建
8. **info.json 向后兼容**：`readInfoJson()` 解析时 `description` 字段缺失应设默认值空字符串
9. **编辑章节元数据**：order 修改时需确保不与其他章节的 order 冲突，修改后需刷新列表保证排序正确
10. **PromptManager 变量注入**：`{{description}}` 为空的处理 — 如果项目无简介，显示空字符串或"暂无简介"

---

## 七、实现顺序建议

1. **Phase 1**：info.json 同步 + 提示词注入 + AGENTS.md 更新（独立模块，低风险）
2. **Phase 2**：FileManager 新增 `getFileTree()` 等文件操作方法
3. **Phase 3**：WritingViewModel 扩展（文件浏览状态和方法）
4. **Phase 4**：WritingScreen UI 改造（FileListPanel + 各弹窗组件）
5. **Phase 5**：字符串资源国际化
6. **Phase 6**：编译验证 `./gradlew :app:assembleDebug`

---

## 八、UI 状态示意

```
┌──────────────────────────────────────────────────────────────────┐
│  [返回]  《书名》                                    字数: 1234  │
├──────────────────────────────────────────────────────────────────┤
│  [写作] [对话]                                                    │
├────────────┬─────────────────────────────────────────────────────┤
│ 章节 ▼ ⚙ + │                                                     │
│ ─────────── │                                                     │
│ ▲▼ 第一章   │  # 第一章                                          │
│     [删除]  │                                                     │
│ ▲▼ 第二章   │  正文内容...                                       │
│     [删除]  │                                                     │
│ ─────────── │                                                     │
│  2 章节     │                                                     │
├────────────┴─────────────────────────────────────────────────────┤
│  章节 2/5 | 总字数: 12,345                                       │
└──────────────────────────────────────────────────────────────────┘

  ┌─────────────┐         ┌──────────────┐       ┌──────────────────┐
  │ 章节 ▼      │         │ 添加文件      │       │ 修改文件名        │
  │ ─────────── │         │ 添加文件夹    │       │ 修改元数据 (章节)  │
  │ 章节    ←当前│        └──────────────┘       └──────────────────┘
  │ 设定         │
  │ 时间线       │
  │ 记录         │
  │ 配置         │
  └─────────────┘
```

---

**文档版本**: 1.0  
**创建日期**: 2026-05-09  
**作者**: AI Agent
