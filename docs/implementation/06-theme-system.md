# 主题系统 (Theme System)

## 功能概述

实现 Material Design 3 深蓝色主题方案，支持浅色/深色双模式，完整的 semantic color scheme、Shape 系统、13 级 Typography 阶梯。通过 Jetpack Compose 的 `MaterialTheme` 统一管理应用外观。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| Theme.kt | `ui/theme/Theme.kt` | 主题 Composable，浅色/深色完整方案 |
| Color.kt | `ui/theme/Color.kt` | 完整语义色板 (浅色29色 + 深色29色) + 向后兼容别名 |
| Type.kt | `ui/theme/Type.kt` | 13 级 Material 3 排版阶梯 |
| Shape.kt | `ui/theme/Shape.kt` | Material 3 形状系统 (5级圆角) |

## 核心类/函数

### 颜色定义 (Color.kt)

**向后兼容别名（保留旧 API）:**
```kotlin
val PrimaryDark = Color(0xFF1a237e)
val PrimaryLight = Color(0xFF3949ab)   // 改: 原 #2196f3
val Accent = Color(0xFFff9800)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)
val TextDisabled = Color(0xFF9e9e9e)
val BackgroundLight = Color(0xFFFAFAFA) // 改: 原 #ffffff
val BackgroundDark = Color(0xFF121212)
```

**浅色主题语义色:**
| 角色 | 值 | 用途 |
|------|-----|------|
| LightPrimary | `#1a237e` | 主色 |
| LightPrimaryContainer | `#D1D4FF` | 主色容器 |
| LightSecondary | `#3949AB` | 辅助色 (改为深靛蓝) |
| LightTertiary | `#FF9800` | 强调色 |
| LightSurface | `#FAFAFA` | 表面 (改为浅灰) |
| LightSurfaceVariant | `#E7E0EB` | 表面变体 |
| LightOutline | `#7A757F` | 描边 |

**深色主题语义色:**
| 角色 | 值 | 用途 |
|------|-----|------|
| DarkPrimary | `#C1C6FF` | 主色 (亮化，深底可见) |
| DarkPrimaryContainer | `#192292` | 主色容器 (深蓝) |
| DarkSecondary | `#B7C4FF` | 辅助色 (亮化) |
| DarkTertiary | `#FFB74D` | 强调色 (柔和) |
| DarkSurface | `#121212` | 表面 |
| DarkSurfaceVariant | `#47464F` | 表面变体 |

### 颜色方案 (Theme.kt)
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,               onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,           onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer, onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,             onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer, onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,         onBackground = LightOnBackground,
    surface = LightSurface,               onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,               outlineVariant = LightOutlineVariant,
    error = LightError,                   onError = LightOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,                onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,            onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,              onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer, onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,          onBackground = DarkOnBackground,
    surface = DarkSurface,                onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,  onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,                outlineVariant = DarkOutlineVariant,
    error = DarkError,                    onError = DarkOnError
)
```

### QuickWriterTheme
```kotlin
@Composable
fun QuickWriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // SideEffect 设置状态栏颜色
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,     // 新增: 形状系统
        content = content
    )
}
```

### 形状定义 (Shape.kt)
```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Chip、小标签
    small = RoundedCornerShape(8.dp),         // 按钮、Snackbar
    medium = RoundedCornerShape(12.dp),       // 卡片、对话框
    large = RoundedCornerShape(16.dp),        // 大卡片、模态
    extraLarge = RoundedCornerShape(24.dp)    // 全屏弹窗
)
```

### 排版定义 (Type.kt) — 13 级完整阶梯
```kotlin
val Typography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = Normal,   lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = Normal,   lineHeight = 32.sp),
    headlineSmall  = TextStyle(fontSize = 20.sp, fontWeight = Normal,   lineHeight = 28.sp),
    titleLarge     = TextStyle(fontSize = 20.sp, fontWeight = Bold,     lineHeight = 28.sp),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = SemiBold, lineHeight = 24.sp),
    titleSmall     = TextStyle(fontSize = 14.sp, fontWeight = SemiBold, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = Normal,   lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = Normal,   lineHeight = 20.sp),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = Normal,   lineHeight = 16.sp),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = Medium,   lineHeight = 20.sp),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = Medium,   lineHeight = 16.sp),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = Medium,   lineHeight = 14.sp)
)
```

### TopAppBar 差异化策略
| 页面类型 | 背景色 | 示例页面 |
|---------|--------|---------|
| 主列表页 | `primary` (深蓝) | ProjectListScreen, SettingsMainScreen |
| 编辑/表单页 | `surface` (白色/深色表面) | ProjectCreateScreen, ProjectEditScreen, AiConfigEditScreen |
| 详情/子页 | `surfaceVariant` (浅灰) | ProjectDetailScreen, WritingScreen, AppSettingsScreen, AboutScreen |

## 设计架构

```
┌──────────────────────────────────┐
│          MainActivity             │
│  setContent {                    │
│    QuickWriterTheme(darkTheme) { │ ← 从设置读取 darkTheme = true/false
│      Surface(color=bg) {         │
│        QuickWriterApp()          │
│      }                           │
│    }                             │
│  }                               │
└──────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│       QuickWriterTheme            │
│                                   │
│  ┌──────────────────────┐         │
│  │ Color Scheme          │         │
│  │ (完整语义色)          │─────────┤
│  │  - primary + container│         │
│  │  - secondary          │         │
│  │  - tertiary           │         │
│  │  - surface/variant    │         │
│  │  - outline/variant    │         │
│  └──────────────────────┘         │
│                                   │
│  ┌──────────────────────┐         │
│  │ Typography            │         │
│  │ (13 styles)           │─────────┤
│  └──────────────────────┘         │
│                                   │
│  ┌──────────────────────┐         │
│  │ Shapes                │         │
│  │ (5 levels)            │─────────┤
│  └──────────────────────┘         │
│                                   │
│  SideEffect: 设置状态栏颜色        │
└──────────────────────────────────┘
```

## 数据流

### 主题模式决策
```
UserSettingsRepository.getThemeMode()
    │
    ├─ "system" → isSystemInDarkTheme()
    ├─ "dark"   → true
    └─ "light"  → false
    │
    ▼
QuickWriterTheme(darkTheme = result)
    │
    ├─ darkTheme=true  → DarkColorScheme (亮化色板)
    └─ darkTheme=false → LightColorScheme (深蓝主色)
```

## 数据流

### 主题模式决策
```
UserSettingsRepository.getThemeMode()
    │
    ├─ "system" → isSystemInDarkTheme()
    ├─ "dark"   → true
    └─ "light"  → false
    │
    ▼
QuickWriterTheme(darkTheme = result)
    │
    ├─ darkTheme=true  → DarkColorScheme
    └─ darkTheme=false → LightColorScheme
```

## 关键实现细节

### 状态栏处理
- 使用 `SideEffect` 在每次重组时设置状态栏颜色
- 状态栏颜色 = `colorScheme.primary.toArgb()`
- 浅色主题时状态栏图标为深色 (`isAppearanceLightStatusBars = true`)
- 深色主题时状态栏图标为浅色 (`isAppearanceLightStatusBars = false`)

### 颜色设计原则
- 浅色模式保持品牌深蓝 (#1a237e) 作为主色
- 深色模式使用亮化色 (#C1C6FF) 作为主色，确保在深底 (#121212) 上可读
- 辅助色改为深靛蓝 (#3949AB)，比原 #2196F3 更稳重
- 深色强调色 (#FFB74D) 比浅色 (#FF9800) 更柔和，减少刺眼感
- surface 改为 #FAFAFA（原 #FFFFFF），微灰底色更柔和
- 完整的 `surfaceVariant` / `outline` / `outlineVariant` 支持卡片层次

### 形状系统
- 5 级圆角阶梯贯穿全应用，通过 `MaterialTheme.shapes` 统一访问
- 各组件使用 `MaterialTheme.shapes.medium` 等引用，非硬编码

### 页面过渡动画
- NavHost 全局使用 `fadeIn`/`fadeOut` (250ms) 替代原来的 slide 过渡
- 卡片按压使用 `animateFloatAsState` 缩放动画 (0.97x)

### 向后兼容
- `PrimaryDark`, `PrimaryLight`, `Accent`, `TextPrimary`, `TextSecondary`, `TextDisabled`, `BackgroundLight`, `BackgroundDark` 作为别名保留
- 旧代码直接引用这些常量仍然编译通过

## 已知问题/技术债务

1. 字体大小设置（从设置中读取的 10-24sp 范围值）未在 `QuickWriterTheme` 中全局应用，各页面需单独处理
2. `Typography` 全局使用 `FontFamily.Default`，不支持用户自定义字体族
3. 缺少用户自定义主题色功能（当前主色硬编码）
4. 部分页面仍使用硬编码中文字符串（如 ProjectDetailScreen TopAppBar title），应迁移到 stringResource
