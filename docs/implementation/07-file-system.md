# 文件系统管理 (File System Management)

## 功能概述

管理小说项目在本地文件系统中的目录结构、文件 I/O 操作和 ZIP 导出。所有文件操作在 `Dispatchers.IO` 上执行，使用 UTF-8 编码，包含路径遍历攻击防护。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| FileManager | `util/FileManager.kt` | 文件系统核心操作工具 (518行) |
| ChapterFileHelper | `util/ChapterFileHelper.kt` | 章节文件格式解析 (69行) |

## 核心类/函数

### 目录管理
```kotlin
fun getProjectsRootDirectory(): File    // {filesDir}/projects/
fun getProjectDirectory(projectId: String): File  // {rootDir}/{projectId}/
suspend fun createProjectDirectoryStructure(projectId: String): Result<Unit>
private fun createDirectoryStructureAt(projectDir: File)
```

### 文件 I/O
```kotlin
suspend fun readFileContent(filePath: String): Result<String>
suspend fun writeFileContent(filePath: String, content: String): Result<Unit>
suspend fun createFile(filePath: String): Result<String>
suspend fun createDirectory(dirPath: String): Result<String>
```

### 文件操作
```kotlin
suspend fun deleteFileOrDirectory(path: String): Result<Unit>
suspend fun renameFileOrDirectory(oldPath: String, newPath: String): Result<Unit>
suspend fun fileExists(path: String): Boolean
suspend fun getDirectoryContents(path: String): Result<List<String>>
suspend fun getFileSize(path: String): Long
```

### 项目操作
```kotlin
suspend fun deleteProject(projectId: String): Result<Unit>
fun isPathSafe(path: String): Boolean        // 路径遍历防护
suspend fun zipProjectToFile(projectDirPath: String, outputFile: File): Result<Unit>
suspend fun extractZipTo(zipFile: File, outputDir: File): Result<Unit>  // ZIP 解压
```

### 元数据
```kotlin
fun createInfoJson(projectDir: File, title: String, author: String, genre: String, description: String, createdTime: Long)
fun readInfoJson(projectDir: File): InfoJsonData?  // 解析 info.json

data class InfoJsonData(
    val title: String,
    val author: String,
    val genre: String,
    val description: String,
    val createdTime: Long
)
```

### 常量
```kotlin
companion object {
    private const val PROJECTS_DIR = "projects"
    val NOVEL_GENRES = listOf(
        "玄幻", "奇幻", "历史", "都市", "科幻",
        "武侠", "仙侠", "军事", "悬疑", "恐怖",
        "游戏", "竞技", "同人", "轻小说", "其他"
    )
}
```

### 字数统计
```kotlin
fun countWords(text: String): Int    // companion object，识别中英文混合字数
suspend fun countWordsInFile(filePath: String): Int            // 统计单文件字数（自动剥离 YAML Front Matter）
suspend fun countWordsInDirectory(directoryPath: String, extensions: List<String> = listOf(".md")): Int  // 递归统计目录字数
```

### 文件树
```kotlin
suspend fun getFileTree(directory: File): List<FileTreeItem>

data class FileTreeItem(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val children: List<FileTreeItem> = emptyList(),
    val size: Long = 0
)
```

### 封面辅助
```kotlin
fun getCoverImagePath(projectId: String): String
fun hasCoverImage(projectId: String): Boolean
```

### 其他
```kotlin
suspend fun directoryExists(path: String): Boolean     // 检查目录是否存在
```

## 项目目录结构

```
{filesDir}/projects/{projectId}/
├── info.json                # 项目元数据 (JSON)
├── 正文/                    # 正文章节 (*.md)
├── 设定/
│   ├── 人物/
│   ├── 地点/
│   ├── 组织/
│   └── 物品/
├── 时间线/
├── 记录/
└── 配置/
    └── 写作规范.md           # 写作规范（空文件）
```

### info.json 格式
```json
{
  "title": "书名",
  "author": "作者",
  "genre": "类型",
  "createdTime": "2026-04-28T12:00:00.000Z",
  "version": "1.0"
}
```
- `createdTime` 使用 ISO 8601 UTC 格式
- `version` 固定为 `"1.0"`

## 设计架构

```
┌─────────────────────────────────────┐
│           FileManager                │
│                                      │
│  ┌──────────────────────┐           │
│  │ 目录操作               │           │
│  │ - getProjectsRootDir  │           │
│  │ - getProjectDirectory │           │
│  │ - createStructure     │           │
│  │ - createDir/createFile│           │
│  └──────────────────────┘           │
│                                      │
│  ┌──────────────────────┐           │
│  │ 文件 I/O              │           │
│  │ - readFileContent     │           │
│  │ - writeFileContent    │           │
│  │ (UTF-8, IO Dispatcher)│           │
│  └──────────────────────┘           │
│                                      │
│  ┌──────────────────────┐           │
│  │ 导出                   │           │
│  │ - zipProjectToFile    │           │
│  │ (walkTopDown + ZIP)   │           │
│  └──────────────────────┘           │
│                                      │
│  ┌──────────────────────┐           │
│  │ 安全                   │           │
│  │ - isPathSafe          │           │
│  │ (canonicalPath 验证)   │           │
│  └──────────────────────┘           │
└─────────────────────────────────────┘
```

## 数据流

### 项目目录创建
```
ProjectManagementUseCase.createProject()
    │
    ▼
fileManager.createProjectDirectoryStructure(projectId)  [IO]
    │
    ├─ getProjectDirectory(projectId) → 创建根目录
    ├─ 创建 8 个子目录 (正文/设定/人物/地点/组织/物品/时间线/记录/配置)
    ├─ createWritingRulesFile() → 配置/写作规范.md (空文件)
    │
    ▼
fileManager.createInfoJson(projectDir, title, author, genre, description, createdTime)
    │
    └─ 写入 info.json (UTF-8, 格式化 JSON)
```

### ZIP 导出流程
```
ProjectDetailViewModel.performExport()
    │
    ▼
useCase.exportProjectAsZip(projectId, outputFile)  [IO]
    │
    ├─ 获取 project.storagePath (使用数据库值，不重新计算)
    ├─ 检查目录是否存在 (不存在 → 报错)
    ├─ walkTopDown() 收集所有文件和目录
    ├─ 检查是否为空目录 (fileCount==0 && dirCount<=1)
    │   └─ 若为空 → createDirectoryStructureAt() 就地补建
    ├─ 创建 ZipOutputStream
    ├─ 遍历文件：
    │   ├─ 目录 → putNextEntry("dir/")
    │   └─ 文件 → BufferedInputStream + copyTo (8KB buffer)
    └─ 日志记录条目数和输出大小
```

### ZIP 导入流程
```
ProjectManagementUseCase.importProjectFromZip()
    │
    ├─ 将 ZIP 从 URI 复制到临时文件 (cacheDir/import_{uuid}/project.zip)
    │
    ▼
fileManager.extractZipTo(zipFile, extractDir)  [IO]
    │
    ├─ ZipInputStream 逐条目遍历
    ├─ 目录 → entry.name.endsWith("/") → mkdirs()
    ├─ 文件 → BufferedOutputStream (8KB buffer)
    ├─ 每个条目 closeEntry() 后继续 nextEntry
    └─ 失 败时删除 extractDir (deleteRecursively)
    │
    ▼
fileManager.readInfoJson(extractDir)
    │
    ├─ 检查 info.json 文件是否存在
    ├─ JSONObject 解析 title/author/genre/createdTime
    └─ 返回 InfoJsonData；解析失败返回 null
    │
    ▼
fileManager.getProjectDirectory(projectId) → 创建新目录
extractDir.copyRecursively(targetDir) → 迁移文件
```

## 关键实现细节

### 路径安全验证
```kotlin
fun isPathSafe(path: String): Boolean {
    val projectsRoot = getProjectsRootDirectory().canonicalPath
    val targetPath = File(path).canonicalPath
    return targetPath.startsWith(projectsRoot)
}
```
使用 `canonicalPath` 化解 `../` 等路径遍历攻击。

### 存储路径规范（重要）
**必须使用数据库中的 `ProjectEntity.storagePath`，禁止运行时重新计算路径。**

`FileManager.getProjectDirectory(projectId)` 每次调用都会基于 `context.filesDir` 实时计算并自动创建目录。若 `filesDir` 因应用重装等变化，将导致与数据库中的 `storagePath` 不一致。

### 协程调度
所有文件 I/O 操作均通过 `withContext(Dispatchers.IO)` 在 IO 线程上执行，避免阻塞 UI 线程。

### 错误处理
- 写文件前自动创建父目录 (`file.parentFile?.mkdirs()`)
- 错误返回 `Result.failure(Exception)`，包含资源字符串错误消息
- ZIP 导出失败时自动删除不完整的输出文件

### 模板文件内容
创建项目时生成的模板文件：
- `配置/写作规范.md` — 空文件（仅创建，不写入任何内容）
- `info.json` — 含 title/author/genre/description/createdTime/version

### ZIP 空目录处理
导出时如果项目目录为空（只含根目录本身），自动调用 `createDirectoryStructureAt()` 创建完整目录结构，确保导出的 ZIP 包含标准目录树。

## 已知问题/技术债务

1. `getProjectDirectory()` 在每次调用时会自动创建目录，这可能导致意外创建空目录
2. `deleteProject()` 使用 `getProjectDirectory()` 定位目录（而非 `storagePath`），存在与数据库记录不一致的风险
3. 缺少文件操作的事务性保证（如写文件失败时数据库已提交）
4. 文件大小统计未对目录递归进行实时统计，依赖 `ProjectEntity.wordCount` 字段
