# 工具类 (Utilities)

## 功能概述

提供项目各模块共享的通用工具函数和辅助类，包括 ID 生成、日期格式化、封面图片处理、编辑器配置等。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| AppUtils | `util/AppUtils.kt` | 通用工具函数 (77行) |
| CoverImageProcessor | `util/CoverImageProcessor.kt` | 封面图片处理 (93行) |
| AppEditorConfig | `util/AppEditorConfig.kt` | 编辑器配置实现 (18行) |
| FileManager | `util/FileManager.kt` | 文件系统管理 (465行) |
| ChapterFileHelper | `util/ChapterFileHelper.kt` | 章节文件格式 (59行) |
| UiText | `util/UiText.kt` | 国际化文本封装 (30行) |
| LocaleHelper | `util/LocaleHelper.kt` | 语言环境切换 (85行) |
| StreamParser | `util/StreamParser.kt` | SSE 流式响应解析，兼容多格式 (156行) |
| TokenEstimator | `util/TokenEstimator.kt` | Token 近似估算 (14行) |
| PromptManager | `util/PromptManager.kt` | AI 提示词模板管理 (70行) |
| HashUtil | `domain/model/HashUtil.kt` | SHA-256 文件哈希 (20行) |

> 注：FileManager、ChapterFileHelper、UiText、LocaleHelper 各有独立的功能文档，本文档聚焦于 AppUtils、CoverImageProcessor 和 AppEditorConfig 三个工具类。

## AppUtils

### 核心函数

```kotlin
object AppUtils {
    fun generateProjectId(): String                    // UUID 字符串
    fun getCurrentTimestamp(): Long                    // System.currentTimeMillis()
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String
    fun formatRelativeTime(context: Context, timestamp: Long): String
    fun formatFileSize(bytes: Long): String            // B/KB/MB/GB
    fun formatWordCount(context: Context, wordCount: Int): String
    fun isValidEmail(email: String): Boolean
    fun isValidUrl(url: String): Boolean
    fun sanitizeFileName(fileName: String): String     // 替换非法文件名字符
    fun truncateText(text: String, maxLength: Int = 100): String
}
```

### 相对时间格式化
```kotlin
fun formatRelativeTime(context: Context, timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L       → context.getString(R.string.time_just_now)
        diff < 3600_000L     → context.getString(R.string.time_minutes_ago, diff / 60_000L)
        diff < 86400_000L    → context.getString(R.string.time_hours_ago, diff / 3600_000L)
        diff < 604800_000L   → context.getString(R.string.time_days_ago, diff / 86400_000L)
        diff < 2592000_000L  → context.getString(R.string.time_weeks_ago, diff / 604800_000L)
        diff < 31536000_000L → context.getString(R.string.time_months_ago, diff / 2592000_000L)
        else                 → context.getString(R.string.time_years_ago, diff / 31536000_000L)
    }
}
```

### 字数格式化
```kotlin
fun formatWordCount(context: Context, wordCount: Int): String {
    return when {
        wordCount > 10000 -> context.getString(R.string.word_count_ten_k, wordCount / 10000.0)
        wordCount > 1000  -> context.getString(R.string.word_count_k, wordCount / 1000.0)
        else              -> context.getString(R.string.word_count_single, wordCount)
    }
}
```

### 文件大小格式化
```kotlin
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024                → "$bytes B"
        bytes < 1024 * 1024        → "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 → "${bytes / (1024 * 1024)} MB"
        else                        → "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
```

### 文件名净化
```kotlin
fun sanitizeFileName(fileName: String): String {
    val invalidChars = Regex("[\\\\/:*?\"<>|]")
    return fileName.replace(invalidChars, "_")
}
```

## CoverImageProcessor

### 核心函数
```kotlin
object CoverImageProcessor {
    private const val COVER_WIDTH = 600
    private const val COVER_HEIGHT = 800
    private const val COVER_QUALITY = 90
    private const val COVER_FILE_NAME = "cover.jpg"

    fun getCoverFilePath(projectDir: String): String
    fun hasCoverImage(projectDir: String): Boolean
    fun getCoverFile(projectDir: String): File
    suspend fun saveCoverImage(context: Context, sourceUri: Uri, projectDir: String): Result<String>
    private fun processBitmap(original: Bitmap): Bitmap
    fun deleteCoverImage(projectDir: String): Result<Unit>
}
```

### 图片处理流程
```
saveCoverImage(context, sourceUri, projectDir)  [IO Dispatcher]
    │
    ├─ ContentResolver.openInputStream(sourceUri)  // 打开图片流
    ├─ BitmapFactory.decodeStream(stream)          // 解码为 Bitmap
    ├─ processBitmap(original)                     // 处理
    │   ├─ 计算缩放比例 (fit max 600×800)
    │   ├─ original.scale(scaledWidth, scaledHeight) // 缩放
    │   ├─ createBitmap(600, 800)                  // 创建画布
    │   ├─ eraseColor(WHITE)                       // 白色背景
    │   ├─ canvas.drawBitmap(scaledBitmap, centered) // 居中绘制
    │   └─ recycle() 释放原始 Bitmap
    ├─ Bitmap.compress(JPEG, 90) → FileOutputStream  // 保存
    ├─ recycle() 释放处理后的 Bitmap
    └─ return Result.success(coverFile.absolutePath)
```

### 设计要点
- 固定画布：600×800 像素，白色背景
- 等比缩放：保持宽高比，按较小边适配
- 居中放置：缩放后的图片在画布上居中
- 质量：JPEG 压缩质量 90
- 文件名：固定 `cover.jpg`

## PromptManager

### 实现
```kotlin
class PromptManager(context: Context) {
    fun resolve(templateKey: String, variables: Map<String, String> = emptyMap()): String
    fun getDefaultAssistantPrompt(): String
    fun getNovelWritingAssistantPrompt(
        title: String,
        author: String,
        genre: String,
        storagePath: String,
        description: String = "",
        writingRules: String = ""
    ): String
    fun getNoProjectAssistantPrompt(): String
}
```

### 模板文件
```
assets/prompts/
├── default_assistant.md          # 无变量：默认助手提示词
├── novel_writing_assistant.md    # 含 {{title}} {{author}} {{genre}} {{description}} {{storagePath}} {{writingRulesContent}} 占位符
└── no_project_assistant.md       # 无变量：非项目场景助手提示词
```

### 设计要点
- **加载时机**：构造函数中读取 `assets/prompts/` 下所有 `.md` 文件到内存 Map
- **占位符语法**：`{{变量名}}` — 调用 `resolve()` 时通过字符串替换注入
- **容错**：模板文件不存在或加载失败时返回空字符串，记录 Timber 警告
- **修改提示词**：直接编辑 `assets/prompts/*.md`，无需改 Kotlin 代码
- **注入方式**：由 `AIChatService` 创建并传入 `SessionManager` 和 `ApiDispatcher`

## AppEditorConfig

### 实现
```kotlin
class AppEditorConfig(
    private val isDark: Boolean,
    private val fontFamily: String = "",
    private val fontSizeSp: Int = 14
) : EditorConfig {
    override fun isDarkModeEnabled(): Boolean = isDark
    override fun getFontFamily(): String = fontFamily
    override fun getEditorForegroundColor(): Int =
        if (isDark) android.graphics.Color.WHITE
        else android.graphics.Color.BLACK
}
```

实现 `EditorConfig` 接口，为 markor-editor 提供配置：
- **isDarkModeEnabled**: 从 Compose 的 `MaterialTheme.colorScheme.background` 计算
- **getFontFamily**: 从用户设置读取（默认空字符串）
- **getEditorForegroundColor**: 深色模式白色，浅色模式黑色
- **fontSizeSp**: 编辑器字号，默认 14sp

## 跨工具类依赖关系

```
AppContainer
    ├── FileManager (context)            → 文件系统操作
    │   ├── CoverImageProcessor          → 封面图片处理
    │   └── AppUtils (间接调用)           → UUID/时间戳
    ├── AppEditorConfig (isDark, font)   → 编辑器配置
    ├── LocaleHelper                     → 语言环境
    ├── UiText                           → 国际化消息
    └── ChapterFileHelper               → 章节格式
```

## 已知问题/技术债务

1. `AppUtils.formatFileSize()` 使用了硬编码字符串（"B", "KB", "MB", "GB"）而非字符串资源
2. `CoverImageProcessor.saveCoverImage()` 处理大图时可能导致 OOM（OutOfMemoryError），缺少采样率控制
3. `AppEditorConfig` 的字体族设置当前固定为 "default"，未与 `UserSettingsRepository.getFontFamily()` 联动
4. `sanitizeFileName()` 使用下划线替换非法字符，但未处理连续多个非法字符或前后空格

---

**文档版本**: 1.1  
**最后更新**: 2026-05-01  
**变更**: 新增 `PromptManager` 类
