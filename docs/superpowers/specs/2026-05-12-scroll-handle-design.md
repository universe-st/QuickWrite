# Scroll Handle — 编辑器滚动控件设计规格

**日期**: 2026-05-12 | **状态**: 已确认

## 概述

在写作编辑器的右侧新增一个可拖动的胶囊形滚动控件，用户可通过拖拽快速定位文档位置，同时该控件也作为当前滚动位置的视觉指示器。

## 功能需求

| ID | 需求 | 详情 |
|----|------|------|
| F1 | 位置指示 | 控件垂直位置反映编辑器当前滚动位置（scrollY / maxScrollY 比例映射） |
| F2 | 拖动控制 | 用户拖动控件时，编辑器内容按绝对位置映射滚动（手指位置 = 文档位置比例） |
| F3 | 自动显示 | 用户手动滚动编辑器内容时，控件以 500ms fade-in 动画显示 |
| F4 | 自动隐藏 | 用户停止操作 3 秒后，控件以 500ms fade-out 动画消失 |
| F5 | 默认隐藏 | 初始状态下控件不可见 |
| F6 | 拖动保持 | 用户拖动控件期间不触发自动隐藏计时器 |

## 视觉规格

| 属性 | 值 |
|------|-----|
| 形状 | 胶囊形 (RoundedCornerShape 4dp，圆角 = 宽度一半) |
| 尺寸 | 视觉: 8dp 宽 × 24dp 高 |
| 触摸热区 | 24dp 宽 (视觉胶囊在热区内水平居中) |
| 颜色 | `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)` |
| 位置 | 贴靠编辑器区域右边缘，垂直位置随滚动变化 |
| 可见性动画 | tween(500ms), alpha 0 ↔ 1 |

## 技术设计

### 方案: Compose 覆盖层 (方案一)

新建组件 `ScrollHandle.kt`，在 `WritingScreen.kt` 的编辑器 Box 内叠加。

### 文件变更

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建 | `presentation/ui/components/ScrollHandle.kt` | 滚动控件 Composable |
| 修改 | `presentation/ui/screens/WritingScreen.kt` | 集成 ScrollHandle |

### 组件 API

```kotlin
@Composable
fun ScrollHandle(
    scrollY: Int,                // 当前编辑器 scrollY
    maxScrollY: Int,             // 最大可滚动范围
    editorHeight: Int,           // 编辑器可见高度
    onScrollTo: (Float) -> Unit, // 拖动回调: 0f..1f 比例
    modifier: Modifier = Modifier,
    isUserScrolling: Boolean,    // 用户是否正在手动滚动
    isHandleDragging: Boolean,   // 用户是否正在拖动控件
    onDragActiveChanged: (Boolean) -> Unit, // 拖动状态变化
)
```

### 可见性逻辑

```kotlin
var targetAlpha by remember { mutableFloatStateOf(0f) }

// 自动隐藏计时器
if (isUserScrolling) {
    targetAlpha = 1f
}
LaunchedEffect(targetAlpha, isHandleDragging) {
    if (targetAlpha == 1f && !isHandleDragging) {
        delay(3000)
        targetAlpha = 0f
    }
}

val alpha by animateFloatAsState(targetAlpha, animationSpec = tween(500))
```

### 数据流

```
HighlightingEditor (View)  scrollY 变化
  → OnScrollChangedListener
    → WritingScreen scrollState (mutableIntStateOf)
      → ScrollHandle 位置计算

用户拖动 ScrollHandle
  → onScrollTo(fraction: Float)
    → editorView.scrollY = maxScrollY * fraction
```

## UI 集成位置

在 `WritingScreen.kt` 的 `EditorContent` 函数中，`Box(Modifier.weight(1f).fillMaxHeight())` 内:

```
Box(Modifier.weight(1f).fillMaxHeight()) {
    MarkorEditor(...)           // 已有
    ScrollHandle(...)           // 新增，fillMaxHeight() + align CenterEnd
}
```
