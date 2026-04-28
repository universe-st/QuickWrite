# 主题系统 (Theme System)

## 功能概述

实现 Material Design 3 深蓝色主题方案，支持浅色/深色双模式，通过 Jetpack Compose 的 `MaterialTheme` 和自定义 `colorScheme` 统一管理应用外观。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| Theme.kt | `ui/theme/Theme.kt` | 主题 Composable，浅色/深色方案 (66行) |
| Color.kt | `ui/theme/Color.kt` | 颜色常量定义 (14行) |
| Type.kt | `ui/theme/Type.kt` | 排版风格定义 (66行) |

## 核心类/函数

### 颜色定义 (Color.kt)
```kotlin
val PrimaryDark = Color(0xFF1a237e)    // 深蓝色主色
val PrimaryLight = Color(0xFF2196f3)   // 浅蓝色辅助色
val Accent = Color(0xFFff9800)         // 橙色强调色

val TextPrimary = Color(0xFF212121)    // 主要文字（浅色主题用）
val TextSecondary = Color(0xFF757575)  // 次要文字
val TextDisabled = Color(0xFF9e9e9e)   // 禁用文字

val BackgroundLight = Color(0xFFFFFFFF)  // 浅色背景
val BackgroundDark = Color(0xFF121212)   // 深色背景
```

### 颜色方案 (Theme.kt)
```kotlin
// 深色方案
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,           // #1a237e
    secondary = PrimaryLight,        // #2196f3
    tertiary = Accent,               // #ff9800
    surface = BackgroundDark,        // #121212
    background = BackgroundDark,     // #121212
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// 浅色方案
private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,           // #1a237e
    secondary = PrimaryLight,        // #2196f3
    tertiary = Accent,               // #ff9800
    surface = BackgroundLight,       // #ffffff
    background = BackgroundLight,    // #ffffff
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,      // #212121
    onSurface = TextPrimary          // #212121
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

    // 设置状态栏颜色和图标外观
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
        WindowCompat.getInsetsController(window, view)
            .isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 排版定义 (Type.kt)
```kotlin
val Typography = Typography(
    titleLarge  = TextStyle(fontSize = 20.sp, fontWeight = Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = SemiBold, lineHeight = 24.sp),
    bodyLarge   = TextStyle(fontSize = 14.sp, fontWeight = Normal, lineHeight = 20.sp),
    bodyMedium  = TextStyle(fontSize = 14.sp, fontWeight = Normal, lineHeight = 20.sp),
    bodySmall   = TextStyle(fontSize = 12.sp, fontWeight = Normal, lineHeight = 16.sp),
    labelLarge  = TextStyle(fontSize = 14.sp, fontWeight = Medium, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = Medium, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontSize = 11.sp, fontWeight = Medium, lineHeight = 16.sp)
)
```

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
│  │ (Light / Dark)        │─────────┤
│  │  - primary: #1a237e   │         │
│  │  - secondary: #2196f3 │         │
│  │  - tertiary: #ff9800  │         │
│  └──────────────────────┘         │
│                                   │
│  ┌──────────────────────┐         │
│  │ Typography            │         │
│  │ (8 styles)            │─────────┤
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
- 主色 (#1a237e) 在浅色和深色模式下保持相同，提供品牌一致性
- 深色模式使用 #121212 作为背景色（Material Design 推荐的深色）
- 深色模式下主要文字使用白色，确保可读性
- 强调色 (#ff9800) 用于突出重要操作按钮和状态标示

## 已知问题/技术债务

1. 字体大小设置（从设置中读取的 10-24sp 范围值）未在 `QuickWriterTheme` 中全局应用，各页面需单独处理
2. `Typography` 全局使用 `FontFamily.Default`，不支持用户自定义字体族
3. 缺少用户自定义主题色功能（当前主色硬编码）
