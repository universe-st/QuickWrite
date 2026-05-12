# QuickWrite UI 全面重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 QuickWrite 从深蓝色工具应用重构为纸墨写作风暖色调写作伴侣

**Architecture:** 分层渐进式重构 — 主题层 (Color/Type/Font/Theme/Shape) → 工具层 → 组件层 → 页面层 → 收尾验证。每层完成后即可编译验证，避免大爆炸式改动。

**Tech Stack:** Kotlin + Jetpack Compose + Material Design 3 + Google Fonts (downloadable) + Coil

---

## Phase 1: 主题基础层

### Task 1: 重写 Color.kt — 新色板

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Color.kt`

- [ ] **Step 1: 替换 Color.kt 为暖色调色板**

将整个文件内容替换为：

```kotlin
package com.universe_st.quickwriter.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 浅色模式色板 — 纸墨写作风 (Warm Literary / Ink & Paper)
// ============================================================

// Primary: 暖墨棕
val LightPrimary = Color(0xFF78716C)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE7E5E4)
val LightOnPrimaryContainer = Color(0xFF44403C)

// Secondary: 灰棕
val LightSecondary = Color(0xFFA8A29E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF5F5F4)
val LightOnSecondaryContainer = Color(0xFF57534E)

// Tertiary: 琥珀
val LightTertiary = Color(0xFFD97706)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF3C7)
val LightOnTertiaryContainer = Color(0xFF92400E)

// Surface / Background
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF5F0E8)
val LightOnSurfaceVariant = Color(0xFF44403C)
val LightBackground = Color(0xFFFFFBEB)
val LightOnBackground = Color(0xFF0F172A)

// Outline
val LightOutline = Color(0xFFD6D3D1)
val LightOutlineVariant = Color(0xFFE7E5E4)

// Error
val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF991B1B)

// Inverse
val LightInverseSurface = Color(0xFF1C1917)
val LightInverseOnSurface = Color(0xFFF5F0E8)
val LightInversePrimary = Color(0xFFA8A29E)

// ============================================================
// 暗色模式色板
// ============================================================

val DarkPrimary = Color(0xFFA8A29E)
val DarkOnPrimary = Color(0xFF44403C)
val DarkPrimaryContainer = Color(0xFF57534E)
val DarkOnPrimaryContainer = Color(0xFFE7E5E4)

val DarkSecondary = Color(0xFFC5BFBA)
val DarkOnSecondary = Color(0xFF44403C)
val DarkSecondaryContainer = Color(0xFF57534E)
val DarkOnSecondaryContainer = Color(0xFFF5F5F4)

val DarkTertiary = Color(0xFFF59E0B)
val DarkOnTertiary = Color(0xFF451A03)
val DarkTertiaryContainer = Color(0xFF92400E)
val DarkOnTertiaryContainer = Color(0xFFFEF3C7)

val DarkSurface = Color(0xFF282421)
val DarkOnSurface = Color(0xFFF5F0E8)
val DarkSurfaceVariant = Color(0xFF3E3A36)
val DarkOnSurfaceVariant = Color(0xFFD6D3D1)
val DarkBackground = Color(0xFF1C1917)
val DarkOnBackground = Color(0xFFF5F0E8)

val DarkOutline = Color(0xFF8D8986)
val DarkOutlineVariant = Color(0xFF3E3A36)

val DarkError = Color(0xFFFCA5A5)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFEE2E2)

val DarkInverseSurface = Color(0xFFF5F0E8)
val DarkInverseOnSurface = Color(0xFF1C1917)
val DarkInversePrimary = Color(0xFF78716C)

// ============================================================
// 向后兼容别名（过渡用，逐步移除）
// ============================================================
@Deprecated("Use MaterialTheme.colorScheme.primary instead", ReplaceWith("MaterialTheme.colorScheme.primary"))
val PrimaryDark = LightPrimary

@Deprecated("Use MaterialTheme.colorScheme.secondary instead", ReplaceWith("MaterialTheme.colorScheme.secondary"))
val PrimaryLight = LightSecondary

@Deprecated("Use MaterialTheme.colorScheme.tertiary instead", ReplaceWith("MaterialTheme.colorScheme.tertiary"))
val Accent = LightTertiary

@Deprecated("Use MaterialTheme.colorScheme.onSurface instead", ReplaceWith("MaterialTheme.colorScheme.onSurface"))
val TextPrimary = LightOnSurface

@Deprecated("Use MaterialTheme.colorScheme.onSurfaceVariant instead", ReplaceWith("MaterialTheme.colorScheme.onSurfaceVariant"))
val TextSecondary = LightOnSurfaceVariant

@Deprecated("Use MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) instead", ReplaceWith("MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)"))
val TextDisabled = Color(0xFF9e9e9e)

@Deprecated("Use MaterialTheme.colorScheme.surface instead", ReplaceWith("MaterialTheme.colorScheme.surface"))
val BackgroundLight = Color(0xFFFAFAFA)

@Deprecated("Use MaterialTheme.colorScheme.inverseSurface instead", ReplaceWith("MaterialTheme.colorScheme.inverseSurface"))
val BackgroundDark = Color(0xFF121212)
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/ui/theme/Color.kt
git commit -m "feat: new warm literary color palette replacing deep blue theme"
```

---

### Task 2: 添加可下载字体配置 + 创建 Font.kt

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Font.kt`
- Modify: `app/src/main/AndroidManifest.xml` (可能需要添加 `<meta-data>` 用于 Google Fonts provider)
- Create: `app/src/main/res/values/font_certs.xml`

- [ ] **Step 1: 创建字体证书资源文件**

创建 `app/src/main/res/values/font_certs.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
    <array name="com_google_android_gms_fonts_certs_dev">
        <item>MIIEqDCCA5CgAwIBAgI...DEV CERT...</item>
    </array>
    <array name="com_google_android_gms_fonts_certs_prod">
        <item>MIIEQzCCAyugAwIBAgI...PROD CERT...</item>
    </array>
</resources>
```

> **注意**: 证书值需从 https://www.gstatic.com/fonts/certs/ 获取。可以使用 Android Studio 的 "Downloadable Fonts" 向导生成，或者跳过此文件先测试。

实际上，Compose 中推荐使用更简单的 `FontFamily(Font(R.font.xxx))` 方式。如果 downloadable fonts 配置复杂，改用 bundling 方式。

- [ ] **Step 2: 创建 Font.kt**

```kotlin
package com.universe_st.quickwriter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.universe_st.quickwriter.R

/**
 * 自定义字体家族 — 纸墨写作风
 *
 * 使用 bundled 字体资源（放在 res/font/ 下）。
 * 如果 res/font/ 下尚无字体文件，会 fallback 到系统默认字体。
 */

// Serif 字体 — 标题/正文
private val CormorantGaramondFamily = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_medium, FontWeight.Medium),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold)
)

private val LibreBaskervilleFamily = FontFamily(
    Font(R.font.libre_baskerville_regular, FontWeight.Normal),
    Font(R.font.libre_baskerville_bold, FontWeight.Bold)
)

// Sans 字体 — 标签/UI
private val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

// Monospace — 代码
private val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Medium
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = CormorantGaramondFamily,
        fontWeight = FontWeight.Medium
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = LibreBaskervilleFamily,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    )
)
```

- [ ] **Step 3: 下载字体文件到 res/font/**

使用提供的脚本下载 Google Fonts：

```bash
# 创建 font 目录
New-Item -ItemType Directory -Force -Path "app\src\main\res\font"

# 从 Google Fonts API 下载 (需要先安装 fonts-download script)
# 如果无法自动下载，请开发者手动放入以下字体文件：
# - cormorant_garamond_regular.ttf, _medium.ttf, _semibold.ttf, _bold.ttf
# - libre_baskerville_regular.ttf, _bold.ttf
# - inter_regular.ttf, _medium.ttf, _semibold.ttf
# - jetbrains_mono_regular.ttf, _medium.ttf
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/ui/theme/Font.kt
git add app/src/main/res/font/
git commit -m "feat: add custom typography with Cormorant Garamond + Libre Baskerville + Inter + JetBrains Mono"
```

---

### Task 3: 重写 Type.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Type.kt`

- [ ] **Step 1: 简化 Type.kt 为兼容层**

由于字体定义已迁移到 `Font.kt`（`AppTypography`），`Type.kt` 改为从 `Font.kt` 重新导出，保持向后兼容：

```kotlin
package com.universe_st.quickwriter.ui.theme

import androidx.compose.material3.Typography

/**
 * 全局 Typography 实例。
 *
 * 字体定义见 [AppTypography]（Font.kt）。
 * 如果字体文件尚未添加到 res/font/，系统会自动 fallback 到默认字体。
 */
val Typography: Typography = AppTypography
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/ui/theme/Type.kt
git commit -m "refactor: simplify Type.kt to delegate to Font.kt AppTypography"
```

---

### Task 4: 更新 Theme.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Theme.kt`

- [ ] **Step 1: 更新 Theme.kt — 添加完整 colorScheme + errorContainer + inverse 色**

```kotlin
package com.universe_st.quickwriter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary
)

@Composable
fun QuickWriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/ui/theme/Theme.kt
git commit -m "feat: update Theme.kt with warm color scheme including errorContainer and inverse colors"
```

---

### Task 5: 微调 Shape.kt + 删除 Gradient.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Shape.kt`
- Delete: `app/src/main/java/com/universe_st/quickwriter/ui/theme/Gradient.kt`

- [ ] **Step 1: 更新 Shape.kt（卡片改为 8dp 圆角）**

```kotlin
package com.universe_st.quickwriter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
```

- [ ] **Step 2: 删除 Gradient.kt**

```bash
Remove-Item -LiteralPath "app\src\main\java\com\universe_st\quickwriter\ui\theme\Gradient.kt"
```

- [ ] **Step 3: 清除所有对 Gradient.kt 的引用**

搜索并替换所有 import `Gradient.kt` 的地方：
- `SplashScreen.kt` — 移除 `import com.universe_st.quickwriter.ui.theme.SplashGradient`
- `ProjectListScreen.kt` — 移除 `import com.universe_st.quickwriter.ui.theme.PrimaryGradient`
- `SettingsScreen.kt` — 移除 `import com.universe_st.quickwriter.ui.theme.PrimaryGradient`

运行搜索:
```bash
rg "SplashGradient|PrimaryGradient|PrimaryGradientHorizontal" --include "*.kt" -l
```

逐个文件替换：
- `SplashGradient` → `MaterialTheme.colorScheme.background` (纯色背景)
- `PrimaryGradient` → `MaterialTheme.colorScheme.primary` (纯色背景)
- `PrimaryGradientHorizontal` → `MaterialTheme.colorScheme.primary`

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git rm app/src/main/java/com/universe_st/quickwriter/ui/theme/Gradient.kt
git add app/src/main/java/com/universe_st/quickwriter/ui/theme/Shape.kt
git add -u
git commit -m "refactor: remove Gradient.kt, replace gradients with solid theme colors"
```

---

## Phase 2: 提取公共工具

### Task 6: 提取 LoadingDots 公共组件

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/LoadingDots.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ChatBubble.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ToolExecutionCard.kt`

- [ ] **Step 1: 创建 LoadingDots.kt**

```kotlin
package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 通用的三点加载动画。
 *
 * @param modifier Modifier for the row container
 * @param dotSize 每个点的大小，默认 6dp（小场景用 5dp）
 * @param spacing 点间距，默认 4dp
 * @param dotColor 点颜色，默认使用 onSurfaceVariant
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
    spacing: Dp = 4.dp,
    dotColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingDots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )
        }
    }
}
```

- [ ] **Step 2: 替换 ChatBubble.kt 中的 LoadingPlaceholder**

在 `ChatBubble.kt` 中：
- 添加 `import com.universe_st.quickwriter.presentation.ui.components.LoadingDots`
- 将 `LoadingPlaceholder` composable 中的三点动画替换为 `LoadingDots()`:

```kotlin
@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingPlaceholder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAlpha"
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.chat_typing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LoadingDots()
    }
}
```

- [ ] **Step 3: 替换 ToolExecutionCard.kt 中的 LoadingDots**

在 `ToolExecutionCard.kt` 中：
- 添加 `import com.universe_st.quickwriter.presentation.ui.components.LoadingDots`
- 将私有 `LoadingDots()` composable 替换为公共版本调用，并传递参数:

```kotlin
// 替换原来的私有 LoadingDots() 调用
LoadingDots(
    dotSize = 5.dp,
    spacing = 3.dp,
    dotColor = MaterialTheme.colorScheme.tertiary
)
```

移除私有的 `LoadingDots()` composable 定义（整个函数体）。

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/LoadingDots.kt
git add -u
git commit -m "refactor: extract LoadingDots as shared component, deduplicate animation code"
```

---

### Task 7: 提取 ToolDisplayHelper 工具函数

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/util/ToolDisplayHelper.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ToolExecutionCard.kt`

- [ ] **Step 1: 创建 ToolDisplayHelper.kt**

```kotlin
package com.universe_st.quickwriter.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R

/**
 * AI 工具调用显示名、加载文本和图标的集中管理。
 * 替代 ToolExecutionCard 中的独立 when 分支。
 */
object ToolDisplayHelper {

    data class DisplayStrings(
        val displayName: String,
        val loadingText: String
    )

    private val toolMap = mapOf(
        "create_file" to ToolDef(
            R.string.tool_name_create_file, R.string.tool_loading_create_file,
            Icons.Outlined.NoteAdd
        ),
        "edit_file" to ToolDef(
            R.string.tool_name_edit_file, R.string.tool_loading_edit_file,
            Icons.Outlined.Edit
        ),
        "delete_file" to ToolDef(
            R.string.tool_name_delete_file, R.string.tool_loading_delete_file,
            Icons.Outlined.DeleteOutline
        ),
        "move_file" to ToolDef(
            R.string.tool_name_move_file, R.string.tool_loading_move_file,
            Icons.Outlined.DriveFileMove
        ),
        "copy_file" to ToolDef(
            R.string.tool_name_copy_file, R.string.tool_loading_copy_file,
            Icons.Outlined.ContentCopy
        ),
        "create_project" to ToolDef(
            R.string.tool_name_create_project, R.string.tool_loading_create_project,
            Icons.Outlined.CreateNewFolder
        ),
        "delete_project" to ToolDef(
            R.string.tool_name_delete_project, R.string.tool_loading_delete_project,
            Icons.Outlined.DeleteForever
        ),
        "update_project_info" to ToolDef(
            R.string.tool_name_update_project_info, R.string.tool_loading_update_project_info,
            Icons.Outlined.Settings
        ),
        "view_file" to ToolDef(
            R.string.tool_name_view_file, R.string.tool_loading_view_file,
            Icons.Outlined.Visibility
        ),
        "search_in_project" to ToolDef(
            R.string.tool_name_search_in_project, R.string.tool_loading_search_in_project,
            Icons.Outlined.Search
        ),
        "get_project_list" to ToolDef(
            R.string.tool_name_get_project_list, R.string.tool_loading_get_project_list,
            Icons.Outlined.FolderOpen
        ),
        "get_project_info" to ToolDef(
            R.string.tool_name_get_project_info, R.string.tool_loading_get_project_info,
            Icons.Outlined.Info
        ),
        "get_folder_structure" to ToolDef(
            R.string.tool_name_get_folder_structure, R.string.tool_loading_get_folder_structure,
            Icons.Outlined.FolderOpen
        )
    )

    private class ToolDef(
        val nameRes: Int,
        val loadingRes: Int,
        val icon: ImageVector
    )

    @Composable
    fun getDisplayStrings(toolName: String): DisplayStrings {
        val def = toolMap[toolName]
        return if (def != null) {
            DisplayStrings(
                displayName = stringResource(def.nameRes),
                loadingText = stringResource(def.loadingRes)
            )
        } else {
            DisplayStrings(displayName = toolName, loadingText = "Executing...")
        }
    }

    fun getIcon(toolName: String): ImageVector {
        return toolMap[toolName]?.icon ?: Icons.Outlined.Info
    }
}
```

- [ ] **Step 2: 更新 ToolExecutionCard.kt 使用 ToolDisplayHelper**

替换三个私有函数为其映射版本：

```kotlin
// 替换原有的 getToolDisplayName(), getToolLoadingText(), getToolIcon()
// 在 ToolExecutionCard() 中使用：
val displayStrings = ToolDisplayHelper.getDisplayStrings(toolName)
val icon = ToolDisplayHelper.getIcon(toolName)
```

移除 `getToolDisplayName`、`getToolLoadingText`、`getToolIcon` 三个私有函数。

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/ToolDisplayHelper.kt
git add -u
git commit -m "refactor: extract ToolDisplayHelper to centralize tool display name/loading text/icon mappings"
```

---

### Task 8: 提取 ProviderDisplayHelper 工具函数

**Files:**
- Create: `app/src/main/java/com/universe_st/quickwriter/util/ProviderDisplayHelper.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/AiConfigScreen.kt`

- [ ] **Step 1: 创建 ProviderDisplayHelper.kt**

```kotlin
package com.universe_st.quickwriter.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R

/**
 * AI 服务提供商显示名的集中管理。
 */
object ProviderDisplayHelper {

    private val providerMap = mapOf(
        "deepseek" to R.string.ai_provider_deepseek,
        "kimi" to R.string.ai_provider_kimi,
        "openai" to R.string.ai_provider_openai,
        "zhipu" to R.string.ai_provider_zhipu,
        "qwen" to R.string.ai_provider_qwen,
        "moonshot" to R.string.ai_provider_moonshot,
        "siliconflow" to R.string.ai_provider_siliconflow
    )

    @Composable
    fun getDisplayName(provider: String): String {
        val resId = providerMap[provider.lowercase()] ?: return provider
        return stringResource(resId)
    }

    fun getProviderList(): List<String> {
        return listOf("deepseek", "kimi", "openai", "zhipu", "qwen", "moonshot", "siliconflow")
    }
}
```

> **注意**: 需要确保 `strings.xml` 中存在以上 `ai_provider_*` 字符串资源。如果不存在，先添加它们。

- [ ] **Step 2: 更新 AiConfigScreen.kt 使用 ProviderDisplayHelper**

在 `AiConfigScreen.kt` 中找到所有 when 分支用于 provider 显示名的位置，替换为 `ProviderDisplayHelper.getDisplayName(provider)`。

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/ProviderDisplayHelper.kt
git add -u
git commit -m "refactor: extract ProviderDisplayHelper to centralize AI provider name mappings"
```

---

## Phase 3: 基础组件重构

### Task 9: 重写 ProjectCard.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ProjectCard.kt`

**变更要点:**
- 移除 `detectTapGestures` 自定义交互，改用标准 Card onClick/onLongClick
- 移除 `TextSecondary` 硬编码颜色，改为 `MaterialTheme.colorScheme.onSurfaceVariant`
- 移除自定义 `scaleAnim`，改用 Material ripple 内置反馈
- 封面比例: 72x96dp → 80x120dp (2:3)
- 卡片圆角: 12dp → 8dp (使用 `MaterialTheme.shapes.small`)
- 类型徽章: `secondaryContainer` 背景

- [ ] **Step 1: 替换 ProjectCard.kt**

```kotlin
package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.util.AppUtils
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: ProjectEntity,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    isCurrentProject: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentProject) 4.dp else 2.dp
        ),
        border = if (isCurrentProject) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(80.dp, 120.dp)) {
                ProjectCoverImage(
                    project = project,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentProject) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = project.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = project.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppUtils.formatRelativeTime(context, project.modifiedTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AppUtils.formatWordCount(context, project.wordCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: 检查调用方 — ProjectListScreen 使用新的 onLongClick**

`ProjectListScreen.kt` 中需要将长按逻辑从 `pointerInput` 模式迁移到 Card 的 `onLongClick` 参数。查找当前如何传递 long-click 并确保适配。

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ProjectCard.kt
git add -u
git commit -m "refactor: rewrite ProjectCard with standard Card onClick, warm colors, 2:3 cover ratio"
```

---

### Task 10: 重写 ChatBubble.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ChatBubble.kt`

**变更要点:**
- `LocalClipboardManager` → `ClipboardManager` (新 API)
- `SmallIconButton` 色调从 `onSurface.copy(alpha=0.5f)` → `onSurfaceVariant`
- 用户气泡: `primaryContainer` 背景 (保持)
- AI 气泡: `surfaceVariant` 背景 (保持)
- 参考块: ref 信息背景从 `primaryContainer.copy(alpha=0.5f)` → `tertiaryContainer`
- 移除私有的 `LoadingDots` 动画代码（已提取到公共组件 LoadingDots.kt）

- [ ] **Step 1: 迁移 ClipboardManager API**

查找所有 `LocalClipboardManager.current` 并在新 Compose BOM 中替换为 `androidx.compose.ui.platform.ClipboardManager`。

```kotlin
// 旧代码
@Suppress("DEPRECATION")
val clipboardManager = LocalClipboardManager.current
clipboardManager.setText(AnnotatedString(content))

// 新代码
val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
clipboardManager.setText(AnnotatedString(content))
```

- [ ] **Step 2: 更新参考块颜色**

将 `UserMessageBubble` 中 ref 信息块的颜色从:
```kotlin
color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
```
改为:
```kotlin
color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
```

- [ ] **Step 3: 更新 SmallIconButton tint**

```kotlin
// 旧
tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

// 新
tint = MaterialTheme.colorScheme.onSurfaceVariant
```

- [ ] **Step 4: 替换 LoadingPlaceholder 中的三点动画**

使用 Task 6 中创建的 `LoadingDots()`:

```kotlin
import com.universe_st.quickwriter.presentation.ui.components.LoadingDots

// 在 LoadingPlaceholder 中替换重复的三点动画代码
LoadingDots()
```

- [ ] **Step 5: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ChatBubble.kt
git commit -m "refactor: update ChatBubble ClipboardManager API, warm tint colors, use shared LoadingDots"
```

---

### Task 11: 重写 ToolExecutionCard.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ToolExecutionCard.kt`

**变更要点:**
- 使用 `ToolDisplayHelper` 获取显示文本和图标
- 移除硬编码 fontSize(sp)，改用 `MaterialTheme.typography.*.fontSize`
- 移除私有 `LoadingDots` composable（使用公共版本）
- `statLabel` 中的硬编码字符串 (如 "new lines", "ID") 替换为 stringResource

- [ ] **Step 1: 替换 tool 显示函数为 ToolDisplayHelper**

```kotlin
// 删除 getToolDisplayName(), getToolLoadingText(), getToolIcon() 三个私有函数
// 在 ToolExecutionCard() 中使用:
val displayStrings = ToolDisplayHelper.getDisplayStrings(toolName)
val icon = ToolDisplayHelper.getIcon(toolName)
```

- [ ] **Step 2: 替换硬编码 fontSize**

查找并替换所有 `fontSize = X.sp` 为 `style = MaterialTheme.typography.xxx`：
- `fontSize = 10.sp` → 使用 `MaterialTheme.typography.labelSmall`
- `fontSize = 11.sp` → 使用 `MaterialTheme.typography.labelSmall`
- `fontSize = 12.sp` → 使用 `MaterialTheme.typography.bodySmall`

- [ ] **Step 3: 使用公共 LoadingDots**

将 `ToolExecutionCard` 中 `LoadingDots()` 的调用改为:

```kotlin
import com.universe_st.quickwriter.presentation.ui.components.LoadingDots

LoadingDots(
    dotSize = 5.dp,
    spacing = 3.dp,
    dotColor = MaterialTheme.colorScheme.tertiary
)
```

移除私有 `LoadingDots()` composable。

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ToolExecutionCard.kt
git commit -m "refactor: use ToolDisplayHelper in ToolExecutionCard, remove hardcoded fonts and colors"
```

---

### Task 12: 重写 SettingsComponents.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/SettingsComponents.kt`

**变更要点:**
- `SettingsSwitchItem`: 修复 Switch 双击问题（Switch 自身处理 check change）
- `SettingsSliderItem`: 值徽章改用 `small` shape 保持一致性
- 所有 `disabled` alpha 统一为 0.38f (M3 标准)

- [ ] **Step 1: 修复 SettingsSwitchItem 双击问题**

将 Row 的 clickable 移除，仅保留 Switch 的 onCheckedChange:

```kotlin
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
```

- [ ] **Step 2: 统一 SettingsSliderItem 值徽章 shape**

```kotlin
// 旧
shape = MaterialTheme.shapes.extraSmall,

// 新
shape = MaterialTheme.shapes.small,
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/SettingsComponents.kt
git commit -m "fix: SettingsSwitchItem double-click issue, unify badge shape"
```

---

### Task 13: 重写 ProjectCoverImage.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ProjectCoverImage.kt`

**变更要点:**
- 文件存在检查从同步 I/O 改为 `LaunchedEffect`
- 回退文本字号使用 `MaterialTheme.typography.titleMedium`
- 移除 `File.exists()` 主线程 I/O

- [ ] **Step 1: 重写 ProjectCoverImage 使用异步文件检查**

```kotlin
package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectCoverImage(
    project: ProjectEntity,
    coverImagePath: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var effectivePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(project.id, coverImagePath) {
        val path = withContext(Dispatchers.IO) {
            coverImagePath
                ?: project.coverImagePath
                ?: run {
                    val coverFile = File(project.storagePath, "cover.jpg")
                    if (coverFile.exists()) coverFile.absolutePath else null
                }
        }
        effectivePath = path
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
    ) {
        if (!effectivePath.isNullOrEmpty()) {
            val coverFile = File(effectivePath!!)
            val lastMod = remember(coverFile) { coverFile.lastModified() }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverFile)
                    .memoryCacheKey("${effectivePath}_$lastMod")
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.project_cover_content_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.title.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ProjectCoverImage.kt
git commit -m "refactor: async file check in ProjectCoverImage, use theme typography for fallback text"
```

---

## Phase 4: 核心页面重构

### Task 14: 重写 ProjectListScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectListScreen.kt`

**变更要点:**
- TopAppBar: 移除 `PrimaryGradient`，改用 `primary` 纯色
- FAB: 从手动 Box + shadow + gradient 改为标准 M3 `FloatingActionButton`，颜色 `tertiary`（琥珀）
- 空状态 Icon: 从 `Icons.Default.Add` 改为更贴合的文字图标（如 `Icons.Outlined.AutoStories`）
- 排序按钮: 添加当前排序的视觉指示
- 使用 `contentWindowInsets` 而非 `WindowInsets(0.dp)`

- [ ] **Step 1: 更新 TopAppBar 和 FAB**

在 `ProjectListScreen.kt` 中:
- 找到 `TopAppBar` 的 `colors` 参数
- 将 `containerColor = Color.Transparent` + `PrimaryGradient` 背景 Box → `colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)`
- 将自定义 FAB Box 替换为:

```kotlin
FloatingActionButton(
    onClick = { onNavigateToCreate() },
    containerColor = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary
) {
    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.project_create_new))
}
```

- [ ] **Step 2: 移除 Gradient.kt 引用**

移除 `import com.universe_st.quickwriter.ui.theme.PrimaryGradient`

- [ ] **Step 3: 更新排序按钮指示器**

在排序按钮旁添加当前排序方式的文字标签:

```kotlin
IconButton(onClick = { onToggleSort() }) {
    BadgedBox(badge = {
        if (currentSortOption != defaultSort) {
            Badge { Text("●") }
        }
    }) {
        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.project_sort))
    }
}
```

- [ ] **Step 4: 更新空状态图标**

```kotlin
// 旧
Icon(Icons.Default.Add, ...)

// 新
Icon(Icons.Outlined.AutoStories, ...)
```

- [ ] **Step 5: 添加 contentWindowInsets**

```kotlin
Scaffold(
    // 移除 contentWindowInsets = WindowInsets(0.dp)
    // 或者设置为:
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
    // ... 需要确认现有布局行为后再调整
)
```

> **注意**: `WindowInsets` 调整需要仔细测试，可能影响现有布局。如果风险较高，暂时保留现有设置并在后续 Task 中统一处理。

- [ ] **Step 6: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectListScreen.kt
git commit -m "refactor: ProjectListScreen warm theme — solid TopAppBar, amber FAB, sort indicator"
```

---

### Task 15: 重写 WritingScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt`

**变更要点:**
- 暗色模式检测: `Color(0xFF121212)` → `isSystemInDarkTheme()`
- TabRow: 迁移到非弃用 API (`TabRow` → `TabRow` 或 `PrimaryNavigationTabRow`)
- 硬编码 `TextSecondary` → `MaterialTheme.colorScheme.onSurfaceVariant`
- 硬编码 fontSize (11.sp, 10.sp) → `MaterialTheme.typography.*.fontSize`
- `contentWindowInsets` 适配
- 文件侧栏: `surfaceVariant` 背景，右边缘圆角

- [ ] **Step 1: 修复暗色模式检测**

搜索 `WritingScreen.kt` 中:
```kotlin
MaterialTheme.colorScheme.background == Color(0xFF121212)
```
替换为:
```kotlin
import androidx.compose.foundation.isSystemInDarkTheme

val isDarkTheme = isSystemInDarkTheme()
```

- [ ] **Step 2: 迁移 TabRow**

查找 `@Suppress("DEPRECATION")` 附近的 TabRow 使用。新版 TabRow API（Material3 1.2+）:

```kotlin
TabRow(selectedTabIndex = selectedTab) {
    Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text(stringResource(R.string.writing_tab_editor)) }
    )
    Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text(stringResource(R.string.writing_tab_chat)) }
    )
}
```

- [ ] **Step 3: 替换硬编码颜色和字号**

```bash
# 搜索所有 TextSecondary 使用
rg "TextSecondary" "WritingScreen.kt"
```

替换为 `MaterialTheme.colorScheme.onSurfaceVariant`。

搜索 `fontSize = 11.sp` 和 `fontSize = 10.sp`，替换为 `MaterialTheme.typography.labelSmall` 或 `MaterialTheme.typography.bodySmall`。

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt
git commit -m "refactor: WritingScreen — fix dark mode detection, migrate TabRow, remove hardcoded colors/fonts"
```

---

### Task 16: 重写 ChatTab.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ChatTab.kt`

**变更要点:**
- `SessionListItem`: 从 `pointerInput` 手动手势 → `combinedClickable`
- 输入框发送按钮: 琥珀色 (`tertiary`)
- 回到底部 FAB: `surfaceContainerHigh` 背景
- 执行工具检查: `pc.startsWith("Executing tool:")` 字符串匹配 → 使用状态字段
- 移除重复的动画代码

- [ ] **Step 1: 替换 SessionListItem 手势处理**

```kotlin
// 旧: Modifier.pointerInput(Unit) { detectTapGestures(...) }
// 新:
Modifier.combinedClickable(
    onClick = { onSelect(session) },
    onLongClick = { onDelete(session) }
)
```

- [ ] **Step 2: 更新发送按钮颜色**

```kotlin
FilledIconButton(
    onClick = { onSend() },
    colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    )
)
```

- [ ] **Step 3: 更新回到底部 FAB**

```kotlin
FloatingActionButton(
    onClick = { /* scroll to bottom */ },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.size(40.dp)
)
```

- [ ] **Step 4: 修复工具执行检查**

搜索 `pc.startsWith("Executing tool:")`，如果该字段来自 ViewModel，建议在 ViewModel 中增加一个专用 `isExecutingTool` 状态字段，而非 UI 层字符串匹配。

如果无法修改 ViewModel，添加 TODO 注释标记。

- [ ] **Step 5: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ChatTab.kt
git commit -m "refactor: ChatTab — combinedClickable, amber send button, warm colors"
```

---

## Phase 5: 次级页面重构

### Task 17: 重写 SettingsScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/SettingsScreen.kt`

**变更要点:**
- TopAppBar: 移除 `PrimaryGradient`，改用 `primary` 纯色
- 底部内容加 padding 避免被系统导航栏遮挡
- 设置分组卡片: `surface` 背景, `medium` 圆角（保持一致）

- [ ] **Step 1: 更新 TopAppBar 颜色**

```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
    )
)
```

- [ ] **Step 2: 移除 Gradient.kt import**

移除 `import com.universe_st.quickwriter.ui.theme.PrimaryGradient`

- [ ] **Step 3: 添加底部安全区 padding**

```kotlin
Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = 80.dp) // 留出导航栏空间
)
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/SettingsScreen.kt
git commit -m "refactor: SettingsScreen — solid TopAppBar, bottom safe area padding"
```

---

### Task 18: 重写 AiConfigScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/AiConfigScreen.kt`

**变更要点:**
- 使用 `ProviderDisplayHelper` 替换 provider when 分支
- TopAppBar: `surfaceVariant` 背景
- 默认模型卡片: `primaryContainer` 背景
- 表单输入框: 标准 8dp 圆角

- [ ] **Step 1: 替换 provider when 分支为 ProviderDisplayHelper**

查找所有 provider 名称映射的 when 分支，替换为:
```kotlin
ProviderDisplayHelper.getDisplayName(provider)
```

- [ ] **Step 2: 更新 TopAppBar**

```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
)
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/AiConfigScreen.kt
git commit -m "refactor: AiConfigScreen — use ProviderDisplayHelper, warm colors"
```

---

### Task 19: 重写 AboutScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/AboutScreen.kt`

**变更要点:**
- 背景: `background` 色
- 版本信息: Label 字体 + `onSurfaceVariant` 色

- [ ] **Step 1: 更新 AboutScreen 颜色**

确保页面背景使用 `MaterialTheme.colorScheme.background`，版本文字使用 `MaterialTheme.colorScheme.onSurfaceVariant` 和 `MaterialTheme.typography.labelMedium`。

- [ ] **Step 2: 编译验证 + Commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/AboutScreen.kt
git commit -m "refactor: AboutScreen warm theme colors"
```

---

### Task 20-22: 重写剩余页面

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectCreateScreen.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectEditScreen.kt`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ProjectDetailScreen.kt`

**变更要点（通用）:**
- TopAppBar: 使用 `primary` 或 `surfaceVariant` 背景（保持一致）
- 移除所有 `TextSecondary` 导入和使用，改为 `MaterialTheme.colorScheme.onSurfaceVariant`
- 硬编码 fontSize → `MaterialTheme.typography.*.fontSize`
- 表单输入框使用 8dp 标准圆角

- [ ] **Step 1: 逐个页面修改**

对每个页面执行相同的修改模式：
1. 搜索并替换 `TextSecondary` → `MaterialTheme.colorScheme.onSurfaceVariant`
2. 搜索 `fontSize = X.sp` 并替换为合适的 `MaterialTheme.typography.*.fontSize`
3. TopAppBar 颜色统一
4. 编译验证

- [ ] **Step 2: 编译验证 + Commit（每页独立提交）**

```bash
# For each file:
./gradlew :app:assembleDebug
git add <file>
git commit -m "refactor: <page name> warm theme colors and typography"
```

---

## Phase 6: 启动页与收尾

### Task 23: 重写 SplashScreen.kt

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/SplashScreen.kt`

**变更要点:**
- 背景: `SplashGradient` → `background` 纯色
- Logo 图标: `Color.White` → `primary`
- 标题字体: 使用 `MaterialTheme.typography.displayLarge`
- 副标题: `MaterialTheme.typography.bodySmall`

- [ ] **Step 1: 替换 SplashScreen 内容**

```kotlin
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    statusText: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayLarge,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.splash_slogan),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
```

- [ ] **Step 2: 编译验证 + Commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/SplashScreen.kt
git commit -m "refactor: SplashScreen warm theme — solid background, primary logo, theme typography"
```

---

### Task 24: 清理 MainScreen.kt 中的 PlaceholderScreen

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/MainScreen.kt`

- [ ] **Step 1: 删除未使用的 PlaceholderScreen composable**

找到 `PlaceholderScreen` composable 函数并删除它（它未在任何路由中使用）。

- [ ] **Step 2: 编译验证 + Commit**

```bash
./gradlew :app:assembleDebug
git add app/src/main/java/com/universe_st/quickwriter/presentation/MainScreen.kt
git commit -m "chore: remove unused PlaceholderScreen composable from MainScreen"
```

---

### Task 25: 全局技术债务清扫

- [ ] **Step 1: 搜索残留的硬编码引用**

```bash
# 搜索 TextSecondary 导入（除了 Color.kt 中的 deprecated 定义）
rg "import com.universe_st.quickwriter.ui.theme.TextSecondary" --include "*.kt" -l

# 搜索硬编码 fontSize
rg "fontSize\s*=\s*\d+\.sp" --include "*.kt" -l

# 搜索 PrimaryGradient / SplashGradient 导入
rg "PrimaryGradient|SplashGradient|PrimaryGradientHorizontal" --include "*.kt" -l
```

逐一检查并修复每个匹配。

- [ ] **Step 2: 搜索 WindowInsets(0.dp)**

```bash
rg "WindowInsets\(0\.dp\)" --include "*.kt" -l
```

评估每个 Scaffold 是否需要修复。优先修复主要页面（ProjectList, Writing, Settings），其他页面如果风险低也修复。

- [ ] **Step 3: 全局编译验证**

```bash
./gradlew :app:assembleDebug
```

- [ ] **Step 4: 如全部通过，Commit**

```bash
git add -u
git commit -m "chore: global sweep — remove remaining hardcoded colors, fonts, and gradient refs"
```

---

## Phase 7: 最终验证

### Task 26: 全面构建验证与测试

- [ ] **Step 1: Clean + Build**

```bash
./gradlew clean
./gradlew :app:assembleDebug
```

- [ ] **Step 2: 检查编译警告**

```bash
./gradlew :app:assembleDebug 2>&1 | Select-String "warning:|deprecated|DEPRECATION"
```

- [ ] **Step 3: 运行单元测试（如果存在）**

```bash
./gradlew test
```

- [ ] **Step 4: 手动验证清单**

在设备/模拟器上安装并检查：
- [ ] 浅色模式：所有页面颜色正确（暖棕 + 琥珀 + 奶油纸）
- [ ] 暗色模式：切换无闪烁，对比度充分
- [ ] 系统字体缩放：布局不破损
- [ ] reduced-motion：动画降级
- [ ] 三语言切换：无布局异常
- [ ] 安全区适配：状态栏/导航栏不遮挡内容

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "chore: final validation — clean build passes, all warnings addressed"
```

---

## 不变更范围

以下文件和模块在此重构中**不做任何修改**:
- `data/` 目录下所有文件 (Entity, DAO, Repository, Database)
- `domain/` 目录下所有文件 (UseCase, Model)
- `di/AppContainer.kt`
- `markor-editor/` 模块
- `presentation/viewmodel/` 目录下所有 ViewModel（除非为了 API 变更而必须修改）
- `util/FileManager.kt`, `util/ChapterFileHelper.kt`, `util/AppUtils.kt`, `util/LocaleHelper.kt`
- `res/values/strings.xml` 及翻译文件 (除非需要添加新的 provider 字符串)
- `gradle/` 构建配置

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 字体文件缺失导致编译失败 | Task 2 前确保字体文件到位；如无法获取，使用 `FontFamily.Default` fallback |
| ViewModel 代码引用了已删除的 Gradient API | 编译时捕获；ViewModel 层不改颜色，由 UI 层处理 |
| 暗色模式下新色板对比度不足 | Task 26 手动验证 dark theme |
| TabRow 新版 API 与旧版不兼容 | 保留 `@Suppress("DEPRECATION")` 并添加 TODO，不阻塞重构 |
| ChatTab 工具检查字符串硬编码 | 如 ViewModel 改动风险高，标记 TODO 延后处理 |
