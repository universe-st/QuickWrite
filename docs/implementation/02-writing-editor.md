# 写作编辑器 (Writing Editor)

## 功能概述

提供小说正文的写作工作台，集成 markor-editor 引擎实现 Markdown 语法高亮编辑。支持编辑器/聊天双标签页、章节列表侧滑面板、自动保存等功能。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| WritingScreen | `presentation/ui/screens/WritingScreen.kt` | 写作页面 UI (657行) |
| WritingViewModel | `presentation/viewmodel/WritingViewModel.kt` | 写作状态管理 (408行) |
| MarkorEditor | `markor-editor/.../MarkorEditor.kt` | Compose 编辑器包装器 |
| EditorConfig | `markor-editor/.../EditorConfig.kt` | 编辑器配置接口 |
| AppEditorConfig | `util/AppEditorConfig.kt` | App 端配置实现 |
| HighlightingEditor | `markor-editor/.../HighlightingEditor.java` | 核心编辑器控件 (611行) |
| AutoTextFormatter | `markor-editor/.../AutoTextFormatter.java` | 自动格式化 |
| LineNumbersView | `markor-editor/.../LineNumbersView.java` | 行号显示 |
| SyntaxHighlighterBase | `markor-editor/.../SyntaxHighlighterBase.java` | 语法高亮基类 |
| MarkdownSyntaxHighlighter | `markor-editor/.../MarkdownSyntaxHighlighter.java` | Markdown 高亮 |
| PlaintextSyntaxHighlighter | `markor-editor/.../PlaintextSyntaxHighlighter.java` | 纯文本高亮 |
| TextViewUndoRedo | `markor-editor/.../TextViewUndoRedo.java` | 撤销/重做 |

## 核心类/函数

### WritingUiState
```kotlin
sealed class WritingUiState {
    object NoProject : WritingUiState()      // 未设置当前项目
    object Loading : WritingUiState()         // 加载中
    data class Success(
        val project: ProjectEntity,
        val chapters: List<ChapterFileInfo>,
        val currentChapterIndex: Int,
        val editorContent: String,
        val wordCount: Int,
        val selectedTab: Int,                 // 0=Editor, 1=Chat
        val isSaving: Boolean,
        val isDirty: Boolean,
        val autoSaveImmediately: Boolean,
        val saveMessage: UiText?
    ) : WritingUiState()
    data class Error(val message: UiText) : WritingUiState()
}
```

### ChapterFileInfo
```kotlin
data class ChapterFileInfo(
    val fileName: String,      // 文件名 (如 "ch01.md")
    val title: String,         // 章节标题
    val order: Int,            // 排序序号
    val volume: String,        // 所属分卷
    val summary: String,       // 内容摘要
    val filePath: String       // 文件绝对路径
)
```

### 编辑器组件
```kotlin
// MarkorEditor.kt
@Composable
fun MarkorEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editorConfig: EditorConfig,
    highlightingMode: HighlightingMode = HighlightingMode.MARKDOWN,
    enabled: Boolean = true
)
```

### EditorConfig 接口
```kotlin
interface EditorConfig {
    fun isDarkModeEnabled(): Boolean
    fun getFontFamily(): String
    fun getEditorForegroundColor(): Int
    fun getTabWidth(): Int
    fun isSpellingRedUnderlineEnabled(): Boolean
    fun isDebugEnabled(): Boolean
}
```

## 设计架构

```
┌─────────────────────────────────────────────┐
│              WritingScreen                   │
│  ┌───────────┐  ┌───────────┐              │
│  │ Editor Tab│  │ Chat Tab  │              │
│  │(MarkorEd.)│  │(开发中)    │              │
│  └───────────┘  └───────────┘              │
│  ┌────────────────────┐                     │
│  │ ChapterListPanel   │ (侧滑 220dp)        │
│  │ - ChapterListItem  │                     │
│  └────────────────────┘                     │
│  ┌────────────────────┐                     │
│  │ WritingTopBar      │ (标题/字数/保存)     │
│  │ WritingStatusBar   │ (章节进度)          │
│  └────────────────────┘                     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────┴──────────────────────────┐
│           WritingViewModel                   │
│  - loadCurrentProject()                     │
│  - loadChapters()                           │
│  - selectChapter() / createNewChapter()     │
│  - saveCurrentChapter() (自动/手动)          │
│  - moveChapter() / deleteChapter()          │
│  - countWords()                             │
└──────────────────┬──────────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     │             │             │
┌────┴─────────┐ ┌┴──────────┐ ┌┴──────────────┐
│ProjectManage.│ │FileManager│ │SettingsUseCase│
│mentUseCase   │ │(读写.md)  │ │(currentProject│
│(章节CRUD)    │ │            │ │ 自动保存设置) │
└──────────────┘ └───────────┘ └───────────────┘
```

## 数据流

### 写作页面初始化
1. `WritingViewModel.init` 调用 `loadCurrentProject()`
2. 读取 `settingsUseCase.getCurrentProjectId()` -> 若为 null → `NoProject` 状态
3. 若存在 project → `loadChapters()` 列出 `正文/` 目录下的 `.md` 文件
4. 对每个文件调用 `ChapterFileHelper.parseChapterContent()` 解析 YAML Front Matter
5. 按 `order` 字段排序章节列表
6. 自动选中第一个章节，加载其内容到编辑器

### 编辑器内容变更
1. 用户在 `MarkorEditor` 中输入 → `TextWatcher` 回调 → `onValueChange`
2. ViewModel 的 `updateEditorContent(content)` 被调用
3. 设置 `isDirty = true`
4. 更新字数统计：`countWords(content)` 计算中文字符数 + 英文单词数

### 自动保存机制
```
┌─────────────────────────────────────────┐
│         自动保存决策树                      │
└─────────────────────────────────────────┘
                    │
           isDirty? (内容已修改)
                    │
         ┌─────────┴──────────┐
        Yes                  No → 跳过
         │
    autoSaveImmediately?
         │
    ┌────┴────┐
   Yes       No
    │         │
   1.5秒     间隔定时器
   防抖保存    (从设置读取)
    (cancel      ↓
  然后重新   autoSaveTimerJob
  launch)   定时触发 save
```

### 章节排序
- `moveChapter(fromIndex, toIndex)` 交换两个章节文件 YAML front matter 中的 `order` 值
- 不重命名文件，仅修改文件内容中的 `order: N` 行
- 更新后重新加载章节列表以反映新排序

## 关键实现细节

### markor-editor 集成
- `MarkorEditor` 是 Compose wrapper，通过 `AndroidView` 包装 Java 的 `HighlightingEditor`
- `HighlightingEditor` 继承 `AppCompatEditText`，实现区域化语法高亮
- 高亮策略：8行滚动阈值，超过时只高亮可见区域（性能优化）
- 异步高亮：使用线程池 (`ExecutorService`) 在后台计算高亮 span
- 语法高亮模式：`MARKDOWN`（使用 `MarkdownSyntaxHighlighter`）和 `PLAINTEXT`（使用 `PlaintextSyntaxHighlighter`）
- 外部赋值同步：`update` lambda 中 `view.setText(value)` 仅在文本内容实际不同时执行，避免光标跳动

### 字数统计
```kotlin
fun countWords(text: String): Int {
    var count = 0
    for (char in text) {
        when {
            char.code in 0x4E00..0x9FFF -> count++  // CJK 统一汉字
            char.code in 0x3000..0x303F -> count++  // CJK 标点
            char.code in 0xFF00..0xFFEF -> count++  // 全角字符
            char.isWhitespace() -> { /* skip */ }
            else -> { /* English word counting via split */ }
        }
    }
    // 英文单词通过 split("\\s+") 统计
}
```

### Chat 标签页
当前显示 "Under Development" 占位内容，后续将集成 AI 对话功能。

## 已知问题/技术债务

1. Chat 标签页仅占位，AI 对话功能尚未实现
2. 编辑器未支持自定义字体大小设置的应用（需要在 `AppEditorConfig` 中同步）
3. 自动保存的防抖时间 (1.5秒) 是硬编码的，未支持用户配置
4. 编辑器需要显式处理配置变更（如屏幕旋转）以保持状态
