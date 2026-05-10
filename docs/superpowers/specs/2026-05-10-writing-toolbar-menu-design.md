# Writing Toolbar Menu Design

**Date**: 2026-05-10 | **Status**: Approved

## Summary

将 WritingScreen 顶部工具栏的保存按钮替换为 "+" 下拉菜单，菜单包含保存（条件显示）、复制全文、复制纯文本三个选项。

## Design

### 1. WritingTopBar 改动

- **删除**：现有的 `Save` IconButton（包含条件显示逻辑 `showSaveButton`）
- **新增**：`Add` IconButton，点击展开 `DropdownMenu`

### 2. 菜单结构

```
[+] → DropdownMenu
  ├── 保存           (仅 autoSaveImmediately != true)
  ├── ──────────     (分隔线, 仅保存项存在时)
  ├── 复制全文
  └── 复制纯文本
```

- 保存菜单项的条件与原来 `showSaveButton` 逻辑一致
- 分隔线仅在保存菜单项显示时才出现

### 3. 复制全文

- 复制 `state.editorContent` 全部内容
- 使用 `ClipboardManager.setText(AnnotatedString(text))`
- 显示 Snackbar: "已复制到剪贴板"

### 4. 复制纯文本

- 复制 `state.editorContent` 全部内容
- 先通过 `stripMarkdown()` 过滤所有 Markdown 语法标签
- 过滤的 Markdown 元素：标题 `#`、粗体 `**`/`__`、斜体 `*`/`_`、链接 `[text](url)`、图片 `![alt](url)`、代码块、行内代码、无序列表标记、有序列表标记、引用 `>`、删除线 `~~`、水平线
- 使用 `ClipboardManager.setText(AnnotatedString(text))`
- 显示 Snackbar: "已复制到剪贴板"

### 5. stripMarkdown 工具函数

新增函数 `stripMarkdown(text: String): String`，使用正则逐层过滤 Markdown 语法。

### 6. 字符串资源 (3 个新增)

| Key | EN | zh-CN | zh-TW |
|-----|-----|-------|-------|
| `writing_copy_full_text` | Copy Full Text | 复制全文 | 複製全文 |
| `writing_copy_plain_text` | Copy Plain Text | 复制纯文本 | 複製純文字 |
| `writing_copied` | Copied to clipboard | 已复制到剪贴板 | 已複製到剪貼板 |

### 7. 受影响文件

| 文件 | 改动 |
|------|------|
| `WritingScreen.kt` | WritingTopBar: 删除保存按钮, 新增 + 按钮 + DropdownMenu + 复制逻辑 |
| `res/values/strings.xml` | 新增 3 个英文字符串 |
| `res/values-zh-rCN/strings.xml` | 新增 3 个简体中文字符串 |
| `res/values-zh-rTW/strings.xml` | 新增 3 个繁体中文字符串 |

### 8. 不涉及

- WritingViewModel 无需修改（保存逻辑不变）
- 自动保存逻辑不变
- 编辑器核心不变
