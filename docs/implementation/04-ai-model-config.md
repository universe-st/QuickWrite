# AI 模型配置 (AI Model Configuration)

## 功能概述

管理 AI 服务商的模型配置，支持 OpenAI、Anthropic 和自定义 API 三种服务商类型。提供完整的配置 CRUD、默认模型设置和参数管理。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| AiConfigScreen | `presentation/ui/screens/AiConfigScreen.kt` | AI 配置 UI (513行) |
| SettingsViewModel | `presentation/viewmodel/SettingsViewModel.kt` | 配置管理状态 |
| AiModelConfigRepository | `data/repository/AiModelConfigRepository.kt` | 配置数据仓库 |
| AiModelConfigEntity | `data/local/entity/AiModelConfigEntity.kt` | 数据库实体 |
| AiModelConfigDao | `data/local/dao/AiModelConfigDao.kt` | Room DAO |

## 核心类/函数

### AiModelConfigEntity
```kotlin
@Entity(tableName = "ai_model_configs")
data class AiModelConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val configName: String,          // 配置名称
    val provider: String,            // OpenAI / Anthropic / Custom
    val apiKey: String,              // API 密钥
    val baseUrl: String? = null,     // 自定义 Base URL
    val modelName: String,           // 模型名称 (如 gpt-4)
    val temperature: Float = 0.8f,
    val maxTokens: Int = 2000,
    val topP: Float = 1.0f,
    val topK: Int = 1,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val isDefault: Boolean = false   // 是否为默认配置
)
```

### 支持的服务商
```kotlin
// AiModelConfigRepository.kt
const val PROVIDER_OPENAI = "OpenAI"
const val PROVIDER_ANTHROPIC = "Anthropic"
const val PROVIDER_CUSTOM = "Custom"

const val MODEL_GPT_35_TURBO = "gpt-3.5-turbo"
const val MODEL_GPT_4 = "gpt-4"
const val MODEL_CLAUDE_3 = "claude-3"
```

### 表单验证
```kotlin
data class AiConfigFormData(
    val configName: String = "",
    val provider: String = PROVIDER_OPENAI,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val temperature: Float = 0.8f,
    val maxTokens: Int = 2000,
    val topP: Float = 1.0f,
    val topK: Int = 1,
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val isDefault: Boolean = false,
    val configNameError: String? = null,   // 名称验证错误
    val apiKeyError: String? = null         // API Key 验证错误
)
```

## 设计架构

```
┌────────────────────────────────────┐
│          AI Config UI               │
│  ┌──────────────────────┐          │
│  │ AiConfigListScreen   │          │
│  │ - AiConfigCard × N   │          │
│  │ - FAB (添加配置)      │          │
│  └──────────────────────┘          │
│  ┌──────────────────────┐          │
│  │ AiConfigEditScreen   │          │
│  │ - 表单字段 + 参数滑块 │          │
│  │ - 保存/删除按钮       │          │
│  └──────────────────────┘          │
└────────────────┬───────────────────┘
                 │
┌────────────────┴───────────────────┐
│     SettingsViewModel               │
│  - saveAiConfig() / deleteAiConfig()│
│  - setDefaultAiConfig()             │
└────────────────┬───────────────────┘
                 │
┌────────────────┴───────────────────┐
│     SettingsUseCase                 │
│  - createAiConfig() / deleteConfig()│
└────────────────┬───────────────────┘
                 │
┌────────────────┴───────────────────┐
│  AiModelConfigRepository            │
│  - 名称唯一性验证                    │
│  - 默认配置唯一性保证                │
└────────────────────────────────────┘
```

## 数据流

### 配置列表
1. `AiConfigListScreen` 显示时，ViewModel 加载所有配置
2. `settingsUseCase.getAllAiConfigs()` → `AiModelConfigRepository.getAllConfigs()`
3. DAO 查询：`ORDER BY is_default DESC, id ASC`（默认配置排最前）

### 创建配置
1. 用户在 `AiConfigEditScreen` 填写表单
2. 点击保存 → `SettingsViewModel.saveAiConfig()`
3. 验证：`configName` 和 `apiKey` 非空
4. Repository：检查配置名称唯一性（`isConfigNameUnique()`）
5. 插入数据库 (`insertConfig()`)
6. 如果 `isDefault = true`：先 `clearDefaultConfig()` 清除其他默认，再 `setDefaultConfig(id)` 设置当前为默认

### 设为默认配置
1. 在配置卡片上点击"设为默认"按钮
2. `setDefaultAiConfig(id)` → DAO 执行两个操作：
   - `clearDefaultConfig()` — 将 `is_default = 1` 全部重置为 `0`
   - `setDefaultConfig(id)` — 将指定配置的 `is_default` 设为 `1`

### 删除配置
1. 在编辑页面点击删除按钮（需确认）
2. `deleteAiConfig(id)` 删除记录
3. 如果被删除的是默认配置，自动将列表中的第一个配置提升为默认

## 关键实现细节

### 参数滑块
编辑页面使用 Material 3 `Slider` 组件：
- **Temperature**: 0.1 ~ 2.0，步长 0.1，默认 0.8
- **Max Tokens**: 100 ~ 8000，步长 100，默认 2000
- **Top P**: 0.0 ~ 1.0，步长 0.05，默认 1.0
- **Top K**: 1 ~ 100，步长 1，默认 1
- **Frequency Penalty**: 0.0 ~ 2.0，步长 0.1，默认 0
- **Presence Penalty**: 0.0 ~ 2.0，步长 0.1，默认 0

### Custom 服务商
当 provider 选择 "Custom" 时，额外显示 Base URL 输入框（必填）。OpenAI 和 Anthropic 使用内置的默认 URL，不显示此字段。

### 安全注意
API Key 当前以明文存储在 Room 数据库中，**未加密**。这是已知的安全风险。

## 已知问题/技术债务

1. **API Key 明文存储**：需要实现加密存储（如 EncryptedSharedPreferences 或 Android Keystore）
2. 缺少 API Key 有效性验证（可通过测试 API 调用实现）
3. 网络请求层尚未实现，AI 配置当前仅存储参数
