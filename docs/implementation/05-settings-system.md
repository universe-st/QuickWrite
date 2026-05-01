# 设置系统 (Settings System)

## 功能概述

提供应用各维度设置的持久化管理，包括外观设置（主题、字体、语言）、写作设置（AI 参数）和通用设置（自动保存）。所有设置以键值对形式存储在 Room 数据库中。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| SettingsScreen | `presentation/ui/screens/SettingsScreen.kt` | 设置主页面（含子页面导航） |
| AppSettingsScreen | `presentation/ui/screens/AppSettingsScreen.kt` | 外观与字体设置 UI (369行) |
| WritingSettingsScreen | `presentation/ui/screens/WritingSettingsScreen.kt` | 写作参数设置 UI |
| SettingsViewModel | `presentation/viewmodel/SettingsViewModel.kt` | 设置状态管理 |
| SettingsUseCase | `domain/usecase/SettingsUseCase.kt` | 设置业务逻辑 (171行) |
| UserSettingsRepository | `data/repository/UserSettingsRepository.kt` | 设置数据仓库 (184行) |
| UserSettingEntity | `data/local/entity/UserSettingEntity.kt` | 设置数据库实体 |
| UserSettingDao | `data/local/dao/UserSettingDao.kt` | 设置 DAO |
| SettingsComponents | `presentation/ui/components/SettingsComponents.kt` | 设置 UI 组件库 |

## 核心类/函数

### UserSettingEntity
```kotlin
@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey val key: String,       // 设置键
    val value: String,                  // 设置值（字符串存储）
    val category: String                // 设置分类
)
```

### 设置键常量（UserSettingsRepository）
| 分类 | 键 | 类型 | 默认值 | 说明 |
|------|----|----|------|------|
| `appearance` | `theme_mode` | String | `"system"` | 主题模式 |
| `appearance` | `font_size` | Int | `14` | 字体大小(sp) |
| `appearance` | `font_family` | String | `"default"` | 字体族 |
| `appearance` | `language` | String | `"system"` | 界面语言 |
| `general` | `auto_save_interval` | Int | `5` | 自动保存间隔(分钟) |
| `general` | `auto_save_immediately` | Boolean | `false` | 即时自动保存 |
| `ai_writing` | `use_model_config` | Boolean | `true` | 使用模型配置参数 |
| `ai_writing` | `default_temperature` | Float | `0.8` | 默认温度 |
| `ai_writing` | `default_max_tokens` | Int | `50000` | 默认最大 Token |
| `ai_writing` | `default_top_p` | Float | `1.0` | 默认 Top P |
| `ai_writing` | `max_tool_call_rounds` | Int | `30` | 最大工具调用轮数 |
| `workspace` | `current_project_id` | String? | `null` | 当前项目 ID |

### SettingsSubScreen（设置子页面导航）
```kotlin
sealed class SettingsSubScreen {
    object Main : SettingsSubScreen()
    object AiConfigList : SettingsSubScreen()
    object AiConfigEdit : SettingsSubScreen()
    object WritingSettings : SettingsSubScreen()
    object AppSettings : SettingsSubScreen()
    object About : SettingsSubScreen()
}
```

### UI 组件
```kotlin
// SettingsComponents.kt
SettingsClickItem(title, subtitle, trailingText, onClick)  // 可点击设置项
SettingsSwitchItem(title, subtitle, checked, onCheckedChange)  // 开关设置项
SettingsSliderItem(title, subtitle, value, onValueChange, enabled, ...)  // 滑块设置项
SettingsIntEditItem(title, subtitle, value, onValueChange, enabled)  // 整数编辑框设置项
SettingsSection(title, content)  // 分组标题
SettingsDivider()  // 分隔线
```

## 设计架构

```
┌──────────────────────────────────────┐
│           Settings UI                 │
│  ┌────────────────────────┐          │
│  │ SettingsScreen         │          │
│  │ (内部状态导航)          │          │
│  │  ├─ SettingsMainScreen │          │
│  │  ├─ AppSettingsScreen  │          │
│  │  ├─ WritingSettingsScr.│          │
│  │  ├─ AiConfigListScreen │          │
│  │  ├─ AiConfigEditScreen │          │
│  │  └─ AboutScreen        │          │
│  └────────────────────────┘          │
└────────────────┬─────────────────────┘
                 │
┌────────────────┴─────────────────────┐
│     SettingsViewModel                 │
│  - AppSettingsData 状态管理           │
│  - updateThemeMode/fontSize/...       │
└────────────────┬─────────────────────┘
                 │
┌────────────────┴─────────────────────┐
│     SettingsUseCase                   │
│  - getThemeMode() / setThemeMode()    │
│  - getFontSize() / setFontSize()      │
│  - getLanguage() / setLanguage()      │
│  - AI config CRUD                     │
│  - Current project ID                 │
└────────────────┬─────────────────────┘
                 │
┌────────────────┴─────────────────────┐
│  UserSettingsRepository               │
│  - 类型转换 (String ↔ Int/Float/Bool) │
│  - 默认值回退                         │
└──────────────────────────────────────┘
```

## 数据流

### 设置读取
```
UI 需要显示
    │
    ▼
SettingsViewModel.loadSettings()
    │
    ▼
suspend fun 从 UseCase/Repository 读取各设置
  getThemeMode() → String ("system" | "light" | "dark")
  getFontSize() → Int (10-24)
  getAutoSaveInterval() → Int (0/1/3/5/10/15)
  getLanguage() → String ("system" | "en" | "zh-rCN" | "zh-rTW")
  ...
    │
    ▼
AppSettingsData 状态更新 → UI recompose
```

### 设置写入
```
用户操作 UI
    │
    ▼
SettingsViewModel.updateThemeMode(newMode)
    │
    ▼
settingsUseCase.setThemeMode(newMode)
    │
    ▼
UserSettingsRepository.setSetting(THEME_KEY, newMode, THEME_CATEGORY)
    │
    ▼
UserSettingDao.insertSetting(entity)  // REPLACE 策略
    │
    ▼
UI 自动更新（如果是主题/字体/语言，可能触发 activity.recreate()）
```

## 关键实现细节

### 主题模式切换
- 三种模式：`system`（跟随系统）、`light`（强制浅色）、`dark`（强制深色）
- 设置存储后通过 `QuickWriterTheme(darkTheme = ...)` 参数重新计算
- 主题模式值通过 `ThemeModeDialog` 选择器设置

### 语言切换
- 从 `LanguageDialog` 选择语言后，调用 `LocaleHelper.applyLocale(activity, languageCode)`
- 对于 API 33+，使用 `AppCompatDelegate.setApplicationLocales()`
- 对于旧版 Android，使用 `Configuration.setLocale()` 传统方式
- 切换后调用 `activity.recreate()` 重建界面以应用新语言

### 字体大小
- 范围：10sp ~ 24sp，步长 2sp
- 选项：10, 12, 14, 16, 18, 20, 22, 24
- 存储为字符串，读取时 `.toIntOrNull() ?: 14`

### 自动保存
- 间隔选项：0 分钟（实时）、1、3、5、10、15 分钟
- `auto_save_immediately` 为 true 时，写作页面使用 1.5 秒防抖保存
- `auto_save_immediately` 为 false 时，使用间隔定时器

### 写作参数设置
- Temperature：`SettingsSliderItem` — 0.1 ~ 2.0，步长 0.1
- Max Tokens：`SettingsIntEditItem` — `OutlinedTextField` + `KeyboardType.Number`，仅允许正整数，默认 50000
- Top P：`SettingsSliderItem` — 0.0 ~ 1.0，步长 0.05
- Max Tool Call Rounds：`SettingsIntEditItem` — `OutlinedTextField` + `KeyboardType.Number`，仅允许正整数，默认 30
- `use_model_config` 开关控制是否使用模型自己的参数（当开启时，Temperature/Max Tokens/Top P 输入控件禁用，显示模型配置的值）

### 设置持久化
所有设置通过 Room `@Insert(onConflict = REPLACE)` 策略存储，确保键值唯一且可覆盖。

## 已知问题/技术债务

1. `setFontSize()` 方法中 category 参数错误地使用了 `FONT_FAMILY_KEY` 常量（应为 `FONT_CATEGORY`），但功能上不影响读取
2. 设置读取使用了 `runBlocking` 在 UI 线程上，这在 `attachBaseContext` 中可能导致 ANR 风险
3. 缺少设置变更的事件通知机制（当前的 Flow 订阅不完全）

---

**文档版本**: 1.1  
**最后更新**: 2026-05-01  
**变更**: `default_max_tokens` 默认值 2000 → 50000；新增 `SettingsIntEditItem` 组件替换 Max Tokens 和 Max Tool Call Rounds 的拖曳条；新增 `max_tool_call_rounds` 设置键
