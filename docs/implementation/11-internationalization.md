# 国际化 (Internationalization)

## 功能概述

支持简体中文、繁体中文和英文三种语言的多语言系统。使用 Android 字符串资源 + UiText 封装类实现 ViewModel 层面的国际化消息，通过 LocaleHelper 实现语言切换。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| UiText | `util/UiText.kt` | 国际化文本封装类 (30行) |
| LocaleHelper | `util/LocaleHelper.kt` | 语言环境切换工具 (98行) |
| strings.xml (en) | `res/values/strings.xml` | 英文字符串资源 (375行) |
| strings.xml (zh-rCN) | `res/values-zh-rCN/strings.xml` | 简体中文资源 |
| strings.xml (zh-rTW) | `res/values-zh-rTW/strings.xml` | 繁体中文资源 |

## 核心类/函数

### UiText
```kotlin
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int, val args: Array<out Any> = emptyArray()) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }

    companion object {
        fun from(@StringRes resId: Int, vararg args: Any): UiText = StringResource(resId, args)
        fun from(text: String): UiText = DynamicString(text)
    }
}
```

### LocaleHelper
```kotlin
object LocaleHelper {
    const val CODE_SYSTEM = "system"
    const val CODE_EN = "en"
    const val CODE_ZH_CN = "zh-rCN"
    const val CODE_ZH_TW = "zh-rTW"

    fun applyLocale(activity: Activity, languageCode: String): Boolean
    fun wrapContextForLocale(context: Context, languageCode: String): Context
    private fun applyConfigurationLegacy(activity: Activity, locale: Locale?)
    fun getDisplayLanguageCode(context: Context): String
    fun languageCodeToResourceId(code: String): Int
}
```

## 设计架构

```
┌────────────────────────────────────────────┐
│                 字符串资源                    │
│  values/strings.xml        (英文, 默认)      │
│  values-zh-rCN/strings.xml (简体中文)        │
│  values-zh-rTW/strings.xml (繁体中文)        │
└────────────────────┬───────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────┐
│              使用方式                        │
│                                             │
│  1. Compose UI 中:                          │
│     stringResource(R.string.xxx)            │
│                                             │
│  2. ViewModel 中 (错误/成功消息):            │
│     UiText.StringResource(R.string.xxx)     │
│     UiText.DynamicString(e.message)         │
│                                             │
│  3. 工具类中 (FileManager 等):               │
│     context.getString(R.string.xxx)         │
└────────────────────────────────────────────┘
```

## 数据流

### 语言切换
```
用户在 AppSettingsScreen 选择语言
    │
    ▼
SettingsViewModel.updateLanguage(code)
    │
    ▼
settingsUseCase.setLanguage(code) → UserSettingsRepository
    │
    ▼
LocaleHelper.applyLocale(activity, code)
    │
    ├─ API 33+: AppCompatDelegate.setApplicationLocales()
    └─ API < 33: Configuration.setLocale() + updateConfiguration()
    │
    ▼
activity.recreate()
    │
    ▼
MainActivity.attachBaseContext() 读取新语言 → LocaleHelper.wrapContextForLocale()
    │
    ▼
所有 Composable 重新组合，使用新语言的 stringResource
```

### UiText 使用示例
```kotlin
// ViewModel 中
sealed class ProjectListUiState {
    data class Error(val message: UiText) : ProjectListUiState()
}
// 设置错误:
// _uiState.value = ProjectListUiState.Error(
//     UiText.StringResource(R.string.error_load_projects_failed)
// )
// 或：
// _uiState.value = ProjectListUiState.Error(
//     UiText.DynamicString(e.message ?: "Unknown error")
// )

// Compose UI 中显示
val context = LocalContext.current
Text(text = errorMessage.asString(context))
```

## 关键实现细节

### 字符串资源分类
| 前缀 | 用途 | 示例 |
|------|------|------|
| `common_` | 通用按钮/标签 | `common_save`, `common_cancel`, `common_delete` |
| `nav_` | 导航标签 | `nav_projects`, `nav_writing`, `nav_settings` |
| `project_` | 项目相关 | `project_title`, `project_author`, `project_delete` |
| `writing_` | 写作相关 | `writing_editor`, `writing_new_chapter` |
| `file_` | 文件操作 | `file_error_not_found`, `file_error_read` |
| `settings_` | 设置相关 | `settings_theme`, `settings_font_size` |
| `ai_config_` | AI 配置 | `ai_config_name`, `ai_config_provider` |
| `about_` | 关于页面 | `about_version`, `about_developer` |
| `error_` | 错误消息 | `error_load_projects_failed` |
| `success_` | 成功消息 | `success_project_created` |
| `validation_` | 验证消息 | `validation_title_required` |
| `genre_` | 小说类型 | `genre_xuanhuan`, `genre_qihuan` |
| `format_` | 格式化 | `format_word_count`, `format_file_size` |
| `time_` | 时间相对描述 | `time_just_now`, `time_minutes_ago` |
| `splash_` | 闪屏 | `splash_slogan` |

### 不翻译的内容
以下内容在各个语言资源文件中保持原值：
- 文件系统目录名（如 `正文/`、`设定/`）— 实际文件路径
- 技术符号（如 `?`、`/`、` · `）
- 代码中用做键值的字符串（genre 内部存储值、provider 标识符）
- 文件模板内容（如 AI 指令模板）

### LocaleHelper 语言码映射
```kotlin
CODE_SYSTEM → null (跟随系统)
CODE_EN     → Locale.ENGLISH
CODE_ZH_CN  → Locale.SIMPLIFIED_CHINESE
CODE_ZH_TW  → Locale.TRADITIONAL_CHINESE
```

### API 版本兼容
- **API 33+ (Tiramisu)**: 使用 `AppCompatDelegate.setApplicationLocales()` 进行应用级语言设置
- **API < 33**: 使用传统 `Configuration.setLocale()` + `Resources.updateConfiguration()` 方式

### 硬编码字符串禁令
**绝对禁止**在任何 `.kt` 文件中硬编码用户可见的字符串。所有用户界面文本必须通过 `stringResource()` 或 `UiText.StringResource()` 获取。

## 已知问题/技术债务

1. `LocaleHelper` 中 `CODE_SYSTEM` 时未明确获取系统默认语言，依赖系统自动推断
2. `UiText.StringResource` 的 `args` 参数使用 `contentEquals` 做相等性比较，在某些情况下可能导致看似相同但实际不同的实例问题
3. 语言切换后 `activity.recreate()` 会导致状态丢失，需要所有 ViewModel 正确处理重建
