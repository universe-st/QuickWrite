# 章节管理 (Chapter Management)

## 功能概述

管理小说项目 `正文/` 目录下的章节 `.md` 文件，使用 YAML Front Matter 格式存储章节元数据。支持创建、删除、重排序章节。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| ChapterFileHelper | `util/ChapterFileHelper.kt` | YAML Front Matter 解析和构建 (69行) |
| WritingViewModel | `presentation/viewmodel/WritingViewModel.kt` | 章节操作逻辑 (408行) |
| ProjectManagementUseCase | `domain/usecase/ProjectManagementUseCase.kt` | 章节文件 CRUD 编排 |

## 核心类/函数

### ChapterMeta
```kotlin
data class ChapterMeta(
    val title: String = "",    // 章节标题
    val order: Int = 0,        // 排序序号
    val volume: String = "",   // 分卷信息
    val summary: String = ""   // 内容摘要
)
```

### 解析函数
```kotlin
fun parseChapterContent(content: String): Pair<ChapterMeta, String>
// 返回: (元数据, 正文内容)
// 若无 Front Matter，返回 ChapterMeta() 默认值 + 原内容
```

### 构建函数
```kotlin
fun buildChapterContent(meta: ChapterMeta, body: String): String
// 生成完整 .md 文件内容，包含 YAML Front Matter
```

### 标题提取
```kotlin
fun extractTitleFromBody(body: String): String
// 从正文提取第一个 # heading 作为备选标题
```

## 章节文件格式

```markdown
---
title: "第一章：初入异世"    # 章节标题
order: 1                     # 排序序号
volume: "第一卷：风云初起"   # 分卷信息（可选）
summary: "主角醒来发现自己穿越到了异世界"  # 内容摘要（可选）
---

# 第一章：初入异世

（正文内容...）
```

## 文件位置
所有章节文件存储在 `{projectDir}/正文/` 目录下，以 `.md` 扩展名命名。

## 数据流

### 解析流程
```
文件内容 (String)
        │
        ▼
┌─────────────────┐
│ 按行分割 (lines) │
└────────┬────────┘
         │
   第一行是 "---" ?
         │
    ┌────┴────┐
   Yes       No → 返回 (ChapterMeta(), content)
    │
    ▼
寻找闭合 "---" (从第2行开始)
    │
    ┌────┴────┐
  找到      未找到 → 返回 (ChapterMeta(), content)
    │
    ▼
提取 Front Matter 行 (lines[1] ~ lines[endIndex-1])
    │
    ▼
逐行解析 key: value 对
  - title   → title   (去引号)
  - order   → order   (toIntOrNull)
  - volume  → volume  (去引号)
  - summary → summary (去引号)
    │
    ▼
提取正文 (lines[endIndex+1] ~ 末尾，trimStart)
    │
    ▼
返回 (ChapterMeta(title, order, volume, summary), body)
```

### 构建流程
```
ChapterMeta + body
        │
        ▼
┌──────────────────────┐
│ StringBuilder        │
│ appendLine("---")    │
│ 条件性添加字段:       │
│  title   (若非空)     │
│  order   (若 > 0)    │
│  volume  (若非空)     │
│  summary (若非空)     │
│ appendLine("---")    │
│ appendLine()         │
│ append(body.trimSt)  │
└──────────────────────┘
        │
        ▼
返回完整 .md 内容 (String)
```

## 章节操作

### 创建新章节
1. `WritingViewModel.createNewChapter(title)`
2. 生成文件名格式（由调用方决定，通常基于序号）
3. 调用 `useCase.createChapterFile(projectId, fileName, initialContent)`
4. 初始化内容包含 YAML Front Matter（含 title 和下一个 order 值）
5. 更新项目 `chapterCount`

### 删除章节
1. `WritingViewModel.deleteChapter(index)` 
2. 从文件系统删除 `.md` 文件
3. 更新项目 `chapterCount`
4. 如果删除的是当前选中章节，自动选中相邻章节

### 重排序
1. `WritingViewModel.moveChapter(fromIndex, toIndex)`
2. 读取两个文件的完整内容
3. 分别修改各自的 `order: N` 行
4. 重新写回文件
5. 重新加载章节列表

## 关键实现细节

### 编辑器中的元数据隔离
- 加载章节时：`parseChapterContent()` 分离元数据和正文，仅正文传入编辑器
- 保存章节时：`buildChapterContent(meta, body)` 重新合并元数据和正文

### 标题提取回退
当 YAML Front Matter 中 `title` 为空时，`extractTitleFromBody()` 从正文第一个 `# heading` 行提取标题。

### 引号处理
- 存储时：`title`, `volume`, `summary` 值用双引号包裹（`"value"`）
- 解析时：使用 `removeSurrounding("\"")` 去除首尾引号

## AI 工具集成

以下工具已适配章节文件 YAML Front Matter 格式：

### EditFileTool
- **代码层阻止**编辑 Front Matter 区域（`---` 之间的行）
- 编辑范围若覆盖到 Front Matter 行，返回错误并提示使用 `update_chapter_meta`
- 错误信息包含 `frontMatterStartLine`、`frontMatterEndLine`、`bodyStartLine` 以便 AI 调整编辑范围

### CreateFileTool
- 创建 `正文/` 下的文件时，校验内容是否包含有效的 YAML Front Matter（`title` 和 `order` 为必填）
- 校验失败时返回错误信息及格式帮助
- 描述中指出 `正文/` 文件必须有 front matter，其他目录不限制

### GetChapterMetaTool (`get_chapter_meta`)
新增工具，用于读取章节文件的元数据：
- **输入**: `relativePath`
- **输出**: `title`、`order`、`volume`、`summary`、`frontMatterStartLine`、`frontMatterEndLine`、`bodyStartLine`、`totalLines`
- 返回 Front Matter 边界行号，帮助 AI 确定编辑正文的安全起始行

### UpdateChapterMetaTool (`update_chapter_meta`)
新增工具，用于修改章节元数据：
- **输入**: `relativePath` + 可选 `title`/`order`/`volume`/`summary`
- 仅更新提供的字段，未提供的字段保持原值
- 使用 `ChapterFileHelper.buildChapterContent()` 重建文件，保留正文不变
- 已接入备份/回滚系统（`isModificationTool`、`prepareBackup`、`buildOperationRecord`）

### 工具注册
- `ToolRegistry.kt`: 新增 `GetChapterMetaTool` 和 `UpdateChapterMetaTool`
- `ToolDisplayRegistry.kt`: 新增显示名称和加载文本（中/繁/英三语）
- `strings.xml`: 新增 `tool_name_get_chapter_meta`、`tool_name_update_chapter_meta` 等字符串

### 系统提示
`assets/prompts/novel_writing_assistant.md` 包含完整的章节格式说明、约束规则和 `create_file` 示例。

## 已知问题/技术债务

1. 章节文件以 `order` 字段排序，但文件重命名不自动同步
2. 无批量操作（批量删除/批量重新编号）
3. 缺少章节之间的依赖关系或前置章节概念
