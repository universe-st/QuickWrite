# TXT 小说导入 — 设计文档

**日期**: 2026-05-10  
**状态**: 已确认  
**版本**: 1.0  

---

## 1. 功能概述

支持从 `.txt` 纯文本文件导入小说，自动检测章节标题并根据检测结果切分生成项目。

### 核心行为
- 从项目列表页溢出菜单触发导入
- 弹出文件选择器，筛选 `.txt` 文件
- 选中文件后弹出元数据对话框（书名/作者/类型/章节识别模式）
- 自动检测文件编码（UTF-8/GBK/Big5 等）
- 流式读取，按选定的章节模式正则匹配切分
- 直接创建项目并生成章节文件，不预览
- 无法识别章节时拒绝导入并提示用户

---

## 2. 架构设计

### 2.1 方案选择

采用**方案 B：职责拆分** — 独立 `TxtChapterParser` 专注章节检测和切分，`ProjectManagementUseCase` 负责编排。

### 2.2 新增文件

| 文件 | 职责 |
|------|------|
| `util/TxtChapterParser.kt` | 编码检测 + 章节模式匹配 + 流式文本切分 |
| `util/TxtEncodingDetector.kt` | 编码自动检测（juniversalchardet 封装） |

### 2.3 修改文件

| 文件 | 变更 |
|------|------|
| `domain/usecase/ProjectManagementUseCase.kt` | 新增 `importProjectFromTxt()` 方法 |
| `presentation/viewmodel/ProjectListViewModel.kt` | 新增 TXT 导入状态和回调 |
| `presentation/ui/screens/ProjectListScreen.kt` | 溢出菜单新增「导入TXT」项 |
| `presentation/MainScreen.kt` | 新增 `txtFilePickerLauncher` |
| `presentation/ui/components/` | 新增 `TxtImportDialog` Composable |
| `gradle/libs.versions.toml` | 新增 `juniversalchardet` 依赖 |

### 2.4 数据流

```
用户点击「导入TXT」
  → 系统文件选择器（txtFilePickerLauncher, MIME: text/plain + application/octet-stream）
  → TxtImportDialog（书名/作者/类型/章节模式）
  → ProjectListViewModel.importFromTxt(context, uri, formData)
  → ProjectManagementUseCase.importProjectFromTxt(context, txtUri, title, author, genre, patterns)
    → TxtEncodingDetector.detectEncoding(inputStream)
    → TxtChapterParser.parseChapters(inputStream, charset, patterns)
    → FileManager.createProjectDirectoryStructure(projectId)
    → ChapterFileHelper.buildChapterContent() × N → 写入正文/*.md
    → FileManager.createInfoJson()
    → ProjectRepository.insertProjectDirect()
  → Snackbar 成功 / 失败
```

---

## 3. 章节检测算法

### 3.1 TxtChapterParser 核心逻辑

流式逐行读取：

```
输入: InputStream, Charset, List<ChapterPattern>
输出: List<ChapterSlice> (index, title, bodyText)

1. buffer ← 空行列表
2. chapters ← []
3. preludeLines ← []

4. for each line:
   if line 匹配任一 ChapterPattern:
     if chapters 为空:
       preludeLines = buffer        // 前面是序章
     else:
       chapters.last.body = buffer  // 完成上一章节
     chapters.add(ChapterSlice(title=匹配行原文, body=""))
     buffer ← 空
   else:
     buffer.append(line)

5. chapters.last.body = buffer     // 最后一个章节
6. return (preludeLines, chapters)
```

### 3.2 五种识别模式

| 模式 | 正则表达式 | 示例匹配 |
|------|-----------|----------|
| 第X章/第X回 | `^第[\d零一二三四五六七八九十百千万]+[章回卷节]` | 第一章、第十二回、第108章、第三卷 |
| Chapter X | `^[Cc]hapter\s+\d+` | Chapter 1、Chapter 12、chapter 3 |
| 纯数字标题 | `^\d+[\.、．\s]` | 1. 标题、12、初遇、001 章节名 |
| 分卷标记 | `^第[\d零一二三四五六七八九十百千万]+卷` | 第一卷、第二卷、第三卷 |
| 自定义正则 | 用户输入 | 任意合法 Java 正则 |

- 章节模式可在导入对话框中多选，默认勾选「第X章/第X回」
- 匹配行去除首尾空白后作为章节标题
- 标题超过 100 字符时截断（取前 100 字符）
- 章节标题去重：重名时追加数字后缀 ` (2)`、` (3)`

### 3.3 序章处理

- 第一个章节匹配行之前的内容自动归为「序章」
- 序章为空（文件首行即匹配章节）则不生成
- 序章 title 固定为 `"序章"`，order = 0

### 3.4 章节排序

- 按出现顺序分配 `order` 值：序章 order=0，第一个识别章节 order=1，依次递增
- 分卷模式：将分卷标记行视为卷标题，不占用 order。分卷下第一个章节继承递增 order

### 3.5 边界情况

| 情况 | 处理 |
|------|------|
| 无任何匹配 | 返回 `NoChapterFound` 错误，拒绝导入 |
| 仅 1 个匹配 | 正常导入为 1 个章节 |
| 标题重复 | 追加 ` (2)`、` (3)` 后缀 |
| 空文件 | 拒绝导入 |
| 文件超过 500MB | 拒绝导入，提示文件过大 |
| 章节连片（连续两行都是匹配） | 前一章节 body 为空，正常生成空文件 |

---

## 4. 编码检测

### 4.1 TxtEncodingDetector

使用 `juniversalchardet`（Mozilla UniversalCharsetDetection Java 移植）。

```
检测流程:
1. FileInputStream 打开文件
2. 读取前 4KB 字节作为采样
3. UniversalDetector.detectCharset() 分析
4. 若置信度 >= 50% → 返回检测到的编码名
5. 否则 → 回退 UTF-8
6. 用检测结果创建 InputStreamReader → BufferedReader
```

### 4.2 编码映射

| 检测结果 | Java Charset | 说明 |
|----------|-------------|------|
| UTF-8 | UTF-8 | 直接使用 |
| GB2312 / GBK / GB18030 | GBK | 简中统一映射 |
| BIG5 / Big5-HKSCS | Big5 | 繁中 |
| EUC-KR | EUC-KR | 韩文 |
| SHIFT_JIS | Shift_JIS | 日文 |
| 其他/未知/低置信度 | UTF-8 | 安全回退 |

### 4.3 依赖

```toml
# gradle/libs.versions.toml
juniversalchardet = "com.googlecode.juniversalchardet:juniversalchardet:1.0.3"
```

---

## 5. 数据模型

### 5.1 ChapterSlice（内部数据类）

```kotlin
data class ChapterSlice(
    val index: Int,        // 从 0 开始（序章为 0，第一个识别章节为 1）
    val title: String,     // 章节标题（来自匹配行）
    val body: String,      // 章节正文（纯文本）
    val isVolumeHeader: Boolean = false  // 是否为分卷标记（非实质章节）
)
```

### 5.2 ChapterPattern（章节识别模式）

```kotlin
enum class ChapterPattern(val displayName: String, val regex: String?) {
    CN_CHAPTER("第X章/第X回", """^第[\d零一二三四五六七八九十百千万]+[章回卷节]"""),
    EN_CHAPTER("Chapter X", """^[Cc]hapter\s+\d+"""),
    NUM_HEADING("纯数字标题", """^\d+[\.、．\s]"""),
    VOLUME("分卷标记", """^第[\d零一二三四五六七八九十百千万]+卷"""),
    CUSTOM("自定义正则", null);  // regex 由用户输入提供
}
```

### 5.3 TxtImportData（对话框表单数据）

```kotlin
data class TxtImportData(
    val title: String = "",           // 默认从文件名提取
    val author: String = "",
    val genre: String = "其他",
    val selectedPatterns: Set<ChapterPattern> = setOf(ChapterPattern.CN_CHAPTER),
    val customRegex: String = ""      // 仅当 CUSTOM 被选中时使用
)
```

---

## 6. UI 设计

### 6.1 入口

`ProjectListScreen` 溢出菜单新增「导入TXT」项，位于「导入ZIP」下方。

### 6.2 文件选择器

```kotlin
// MainScreen.kt
val txtFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri -> uri?.let { projectListViewModel.onTxtFileSelected(context, it) } }

// 启动时 MIME 过滤
// arrayOf("text/plain", "application/octet-stream")
// 部分文件管理器将 .txt 上报为 application/octet-stream
```

### 6.3 元数据对话框（TxtImportDialog）

```
┌─────────────────────────────────────┐
│  导入TXT小说                          │
│                                     │
│  书名: [编辑框，默认=文件名去掉.txt]     │
│  作者: [编辑框，必填]                  │
│  类型: [下拉选择，默认 '其他']          │
│                                     │
│  章节识别模式（可多选）:                │
│  ☑ 第X章/第X回                       │
│  ☐ Chapter X / Ch.X                 │
│  ☐ 纯数字标题                          │
│  ☐ 分卷标记                           │
│  ☐ 自定义正则: [输入框]                │
│                                     │
│              [取消]    [开始导入]      │
└─────────────────────────────────────┘
```

- 书名默认 = TXT 文件名去扩展名，可修改
- 至少勾选一种章节模式，否则「开始导入」置灰
- 自定义正则为空时不生效
- 作者为空时「开始导入」置灰

### 6.4 ViewModel 状态

在 `ProjectListUiState` 密封类中新增：

```kotlin
object TxtImporting : ProjectListUiState()
data class TxtImportSuccess(val message: UiText) : ProjectListUiState()
data class TxtImportError(val message: UiText) : ProjectListUiState()
```

### 6.5 导入结果

- 成功：Snackbar 显示 `"导入成功，共 N 个章节"`
- 失败 - 无章节：`"未检测到章节，请检查章节识别模式"`
- 失败 - 文件过大：`"文件过大（超过 500MB）"`
- 失败 - 其他错误：`"导入失败：{错误信息}"`

---

## 7. UseCase 接口

### 7.1 ProjectManagementUseCase.importProjectFromTxt

```kotlin
suspend fun importProjectFromTxt(
    context: Context,
    txtUri: Uri,
    title: String,
    author: String,
    genre: String,
    selectedPatterns: Set<ChapterPattern>,
    customRegex: String? = null
): Result<Unit>
```

流程：
1. 通过 `ContentResolver` 打开 `InputStream`，检查文件大小
2. `TxtEncodingDetector` 检测编码
3. `TxtChapterParser` 流式切分章节
4. 验证至少有一个章节被识别
5. 生成 `projectId`，计算 `storagePath`
6. 创建项目目录结构
7. 逐章节写入 `.md` 文件（序章 + N 个章节）
8. 生成 `info.json`
9. 计算总字数和章节数
10. 创建 `ProjectEntity` 并调用 `insertProjectDirect()` 入库
11. 返回 `Result.success(Unit)` 或 `Result.failure(exception)`

---

## 8. 大文件策略

### 流式读取
- 不将整个文件加载到内存
- 逐行读取，匹配到新章节时立即写盘前一个章节
- 使用 `BufferedReader.useLines {}` 或 `forEachLine`

### 大小限制
- 文件大小上限：500MB
- 通过 `ContentResolver.openInputStream()` 后的 `contentResolver.openFileDescriptor(uri, "r")?.statSize` 判定
- 超过上限返回 `FileTooLarge` 错误

### 章节文件写入
- 以 `FileWriter` + `BufferedWriter` 逐文件写入
- 单个章节最大 50MB（超过则截断并警告）

---

## 9. 字符串资源（i18n）

需在三个语言资源文件中新增以下 key：

| Key | 英文 | 简体中文 | 繁体中文 |
|-----|------|---------|---------|
| `menu_import_txt` | Import TXT | 导入TXT | 匯入TXT |
| `txt_import_title` | Import TXT Novel | 导入TXT小说 | 匯入TXT小說 |
| `txt_import_label_title` | Title | 书名 | 書名 |
| `txt_import_label_author` | Author | 作者 | 作者 |
| `txt_import_label_genre` | Genre | 类型 | 類型 |
| `txt_import_label_patterns` | Chapter detection | 章节识别模式 | 章節識別模式 |
| `txt_import_pattern_cn` | Ch. X / Vol. X | 第X章/第X回 | 第X章/第X回 |
| `txt_import_pattern_en` | Chapter X | Chapter X | Chapter X |
| `txt_import_pattern_num` | Numbered headings | 纯数字标题 | 純數字標題 |
| `txt_import_pattern_vol` | Volume mark | 分卷标记 | 分卷標記 |
| `txt_import_pattern_custom` | Custom regex | 自定义正则 | 自訂義正則 |
| `txt_import_custom_hint` | Enter regex pattern | 输入正则表达式 | 輸入正則表達式 |
| `txt_import_btn_start` | Start Import | 开始导入 | 開始匯入 |
| `txt_import_importing` | Importing... | 正在导入... | 正在匯入... |
| `txt_import_success` | Imported successfully, %1$d chapters | 导入成功，共 %1$d 个章节 | 匯入成功，共 %1$d 個章節 |
| `txt_import_no_chapter` | No chapters detected. Please check the detection patterns. | 未检测到章节，请检查章节识别模式 | 未檢測到章節，請檢查章節識別模式 |
| `txt_import_file_too_large` | File is too large (max 500MB) | 文件过大（超过 500MB） | 文件過大（超過 500MB） |

---

## 10. 章节输出文件格式

每个识别出的章节按现有 YAML Front Matter 格式写入 `正文/` 目录：

```markdown
---
title: "第一章 初入异世"
order: 1
---

# 第一章 初入异世

（正文内容...）
```

- 文件名：`{章节标题}.md`（若标题含非法文件名字符，替换为 `_`）
- 同名文件处理：追加数字后缀 `_2`、`_3`
- volume 和 summary 字段：导入时不填充（导入后用户可手动编辑）

---

## 11. 错误处理

| 错误类型 | 处理方式 |
|---------|---------|
| 用户取消文件选择 | 无操作，返回项目列表 |
| 文件读取权限不足 | Snackbar 显示权限错误 |
| 编码检测失败 | 回退 UTF-8，继续导入 |
| 无任何章节匹配 | 拒绝导入，Snackbar 提示 |
| 文件大小超限 | 拒绝导入，Snackbar 提示 |
| 书名与已有项目重复 | 追加 `(1)`、`(2)` 后缀 |
| 写文件失败 | 清理已创建文件，删除数据库记录，返回错误 |
| 章节标题含非法字符 | 替换为 `_` |
| ContentResolver 读取失败 | 返回 `Result.failure(exception)` |
