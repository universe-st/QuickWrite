# Markor Editor 集成说明

本文档记录了将 [Markor](https://github.com/gsantner/markor) 的文本编辑器控件移植为 QuickWrite 可复用库模块 (`:markor-editor`) 的完整过程、关键决策和踩坑记录。

---

## 一、概述

Markor 是一个 Android Markdown 编辑器，其 `HighlightingEditor`（基于 `AppCompatEditText`）提供了语法高亮、行号、自动格式化等能力。我们将其核心控件提取为独立模块，去除了 Markor 特有能力依赖，通过 `EditorConfig` 接口解耦配置。

**源文件来源**：`externalResp/markor` 目录

**目标模块**：`markor-editor`（Android Library，minSdk 24, compileSdk 36）

---

## 二、模块搭建

### 2.1 基础配置

**`markor-editor/build.gradle.kts`**：
- 插件：`android.library` + `kotlin.compose`（AGP 9.x 不需要 `kotlin.android`，Kotlin 支持已内置）
- compileSdk 36, minSdk 24
- 启用 Compose（`buildFeatures.compose = true`）
- 依赖：AppCompat, Material, Compose BOM, Compose UI, Compose Material3

### 2.2 Gradle 配置变更

根 `build.gradle.kts` 添加：
```kotlin
alias(libs.plugins.kotlin.compose) apply false
alias(libs.plugins.kotlin.android) apply false  // 仅用于其他模块，markor-editor 不需要
alias(libs.plugins.android.library) apply false
```

`gradle/libs.versions.toml` 添加：
- `kotlin-android` 插件定义（`org.jetbrains.kotlin.android`）
- `android-library` 插件定义（`com.android.library`）
- `appcompat` / `material` 库

---

## 三、文件选择策略

### 3.1 复制的源文件

| 文件 | 源路径 | 目标路径 | 许可证 |
|------|--------|----------|--------|
| GsCallback.java | markor/opoc/wrapper/ | 同源 | CC0-1.0 |
| GsTextWatcherAdapter.java | markor/opoc/wrapper/ | 同源 | CC0-1.0 |
| GsTextUtils.java | markor/opoc/format/ | 同源 | CC0-1.0 |
| HighlightingEditor.java | markor/frontend/textview/ | 同源 | Apache 2.0 |
| SyntaxHighlighterBase.java | markor/frontend/textview/ | 同源 | Apache 2.0 |
| TextViewUtils.java | markor/frontend/textview/ | 同源 | Apache 2.0 |
| AutoTextFormatter.java | markor/frontend/textview/ | 同源 | Apache 2.0 |
| LineNumbersView.java | markor/frontend/textview/ | 同源 | Apache 2.0 |
| DraggableScrollbarScrollView.java | markor/frontend/ | 同源 | Apache 2.0 |
| TextViewUndoRedo.java | opoc/frontend/textview/ | 同源 | Apache 2.0 |
| ColorUnderlineSpan.java | markor/format/general/ | 同源 | Apache 2.0 |
| MarkdownSyntaxHighlighter.java | markor/format/markdown/ | 同源 | Apache 2.0 |
| PlaintextSyntaxHighlighter.java | markor/format/plaintext/ | 同源 | Apache 2.0 |
| WrMarkdownHeaderSpanCreator.java | writeily/format/markdown/ | 同源 | MIT |
| WrProportionalHeaderSpanCreator.java | writeily/format/ | 同源 | MIT（需新建） |

**包名保持不变**：Java 文件保留原始包名（`net.gsantner.*`、`other.writeily.*`）
**新文件包名**：Kotlin 文件使用 `com.universe_st.markor_editor`

### 3.2 排除的文件

以下 Markor 特有文件被排除（不适用或过度耦合）：
- `format/highlight/` 目录（复杂的语法高亮加载器）
- `GsContextUtils.java`（大量设备/应用上下文功能）
- `TextCasingUtils.java`（大小写转换，Markor 特有）
- `MarkorSettingActivity.java` 及所有 UI 活动
- 所有与 Markor 应用生命周期相关的文件

---

## 四、适配工作

### 4.1 EditorConfig 接口（核心解耦）

```kotlin
interface EditorConfig {
    fun isSpellingRedUnderlineEnabled(): Boolean = false
    fun isDarkModeEnabled(): Boolean = false
    fun getFontFamily(): String = ""
    fun getEditorForegroundColor(): Int = Color.BLACK
    fun getTabWidth(): Int = 4
    fun isDebugEnabled(): Boolean = false
}
```

替代原版 `AppSettings` 的全部配置读取。默认实现提供安全的默认值，调用方通过自定义实现注入配置。

### 4.2 SyntaxHighlighterBase 适配

**变更**：
- 构造函数从 `AppSettings` 改为 `EditorConfig`
- 添加无参构造函数（null-safe），供子类（MarkdownSyntaxHighlighter, PlaintextSyntaxHighlighter）调用 `super()`
- `configure()` 方法增加 `_editorConfig` 空安全检查
- 移除 `getDefaultHighlighter()` 方法（引用 format/highlight 子包）
- 移除所有 `GsContextUtils` 引用

### 4.3 HighlightingEditor 适配

**变更**：
- 引入 `static EditorConfig _editorConfig` 字段
- 新增 `static setDefaultConfig(EditorConfig)` 方法
- 构造时从 static 字段读取配置
- 移除 `R.string` 资源引用
- 移除 `MainActivity` 调试日志引用

### 4.4 ColorUnderlineSpan 适配

**变更**：
- 内联 `parseHexColor()` 方法（原依赖 GsContextUtils）
- 移除 `GsContextUtils` import

### 4.5 MarkdownSyntaxHighlighter 适配

**变更**：
- 所有 Markor 特有配置（高亮行尾、代码字体等）硬编码为默认值
- 移除 `AppSettings` 引用

### 4.6 PlaintextSyntaxHighlighter 适配

**变更**：
- 大幅简化，仅保留基础 spans（tab 替换、十六进制颜色下划线、链接）
- 移除 format/highlight 子包依赖

### 4.7 TextViewUtils 适配

**变更**：
- 移除 `GsContextUtils` 依赖
- 内联 `formatDateTime()` 调用为 `SimpleDateFormat`
- 移除大小写切换相关方法（`applyTextCasing` 等）

### 4.8 WrProportionalHeaderSpanCreator（新增）

**功能**：满足 `WrMarkdownHeaderSpanCreator` 的依赖需求。
- 接收颜色参数
- `createHeaderSpan(float proportion)` 返回 `RelativeSizeSpan`

---

## 五、Compose 包装组件

**`MarkorEditor.kt`** 提供 Compose 风格的 API：

```kotlin
@Composable
fun MarkorEditor(
    value: String,                    // 文本内容（双向绑定）
    onValueChange: (String) -> Unit,  // 文本变更回调
    modifier: Modifier = Modifier,
    editorConfig: EditorConfig = DefaultEditorConfig(),
    highlightingMode: HighlightingMode = HighlightingMode.PLAINTEXT,
    enabled: Boolean = true,
)
```

**实现要点**：
- 使用 `AndroidView` 嵌入 `HighlightingEditor`
- `factory` 块创建 View 并设置 TextWatcher
- `update` 块同步文本状态和高亮模式
- `HighlightingEditor.setDefaultConfig(config)` 在 factory 中调用

**使用示例**：
```kotlin
MarkorEditor(
    value = content,
    onValueChange = { content = it },
    highlightingMode = HighlightingMode.MARKDOWN
)
```

---

## 六、踩坑记录

### 6.1 AGP 9.x Kotlin 插件变更

**现象**：`org.jetbrains.kotlin.android` 插件报错。

**原因**：Android Gradle Plugin 9.0+ 已内置 Kotlin 支持，不再需要显式 apply Kotlin Android 插件。

**解决**：在 markor-editor/build.gradle.kts 中移除 `alias(libs.plugins.kotlin.android)`。

### 6.2 kotlinOptions 在 AGP 9.x 中不可用

**现象**：`kotlinOptions { jvmTarget = "11" }` 报 `Unresolved reference`。

**原因**：移除 Kotlin Android 插件后，`kotlinOptions` DSL 不再自动可用。

**解决**：使用 `tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }`。

### 6.3 HighlightingEditor 缺少单参构造函数

**现象**：`HighlightingEditor(context)` 编译报错。

**原因**：该 View 只有 `(Context, AttributeSet)` 构造函数。

**解决**：Kotlin 调用改为 `HighlightingEditor(context, null)`。

### 6.4 SyntaxHighlighterBase 无参构造缺失

**现象**：`MarkdownSyntaxHighlighter()` 调用 `super()` 时报错。

**原因**：适配时只保留了带 `EditorConfig` 参数的构造函数。

**解决**：添加无参构造函数，`_editorConfig` 设为 null，并在 `configure()` 中做 null 检查。

### 6.5 WrProportionalHeaderSpanCreator 缺失

**现象**：编译报错找不到符号 `WrProportionalHeaderSpanCreator`。

**原因**：该文件属于 Markor 的 writeily 包，未随 WrMarkdownHeaderSpanCreator 一起复制。

**解决**：在 `other.writeily.format` 包下新建该文件，实现 `createHeaderSpan(float)` 方法。

### 6.6 GsContextUtils.formatDateTime 引用

**现象**：TextViewUtils.java 引用了 `GsContextUtils.formatDateTime()`。

**原因**：原函数依赖 Android Context 获取格式。

**解决**：内联为 `SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())` 调用，移除 GsContextUtils 依赖。

---

## 七、依赖关系

```
HighlightingEditor
├── SyntaxHighlighterBase ← EditorConfig
│   ├── MarkdownSyntaxHighlighter
│   │   └── WrMarkdownHeaderSpanCreator
│   │       └── WrProportionalHeaderSpanCreator
│   └── PlaintextSyntaxHighlighter
├── TextViewUtils
├── AutoTextFormatter
├── TextViewUndoRedo
├── GsTextUtils
├── GsTextWatcherAdapter
├── GsCallback
└── ColorUnderlineSpan
```

编译依赖：
- `androidx.appcompat:appcompat`（HighlightingEditor 继承 AppCompatEditText）
- `com.google.android.material:material`
- Compose BOM + UI + Material3（包装组件用）

---

## 八、后续集成步骤

1. **在 app/build.gradle.kts 添加依赖**：
   ```kotlin
   implementation(project(":markor-editor"))
   ```

2. **创建 EditorConfig 实现**：
   ```kotlin
   class AppEditorConfig(private val settingsRepo: UserSettingsRepository) : EditorConfig {
       override fun isDarkModeEnabled() = settingsRepo.isDarkMode()
       override fun getEditorForegroundColor() = settingsRepo.getEditorTextColor()
       // ...
   }
   ```

3. **在需要使用编辑器的页面集成**：
   ```kotlin
   MarkorEditor(
       value = content,
       onValueChange = { viewModel.updateContent(it) },
       highlightingMode = HighlightingMode.MARKDOWN
   )
   ```

4. **如需行号显示**：在 `MarkorEditor` 上方或左侧叠加 `LineNumbersView`（需额外包装）

---

## 九、许可证信息

| 组件 | 许可证 |
|------|--------|
| HighlightingEditor, SyntaxHighlighterBase, TextViewUtils 等核心文件 | Apache 2.0 |
| GsCallback, GsTextWatcherAdapter, GsTextUtils | CC0-1.0（Public Domain） |
| WrMarkdownHeaderSpanCreator, WrProportionalHeaderSpanCreator | MIT |
| EditorConfig, MarkorEditor（新编写） | 遵循项目自身许可证 |

所有 Apache 2.0 和 MIT 文件的版权头已保留。
