# AI 模型配置 (AI Model Configuration)

## 功能概述

管理 AI 服务商的模型配置，支持 OpenAI、Anthropic、DeepSeek、智谱 (GLM)、Kimi (Moonshot)、硅基流动和自定义 API 七种服务商类型。提供完整的配置 CRUD、默认模型设置和参数管理。

## 关键文件

| 文件 | 路径 | 用途 |
|------|------|------|
| AiConfigScreen | `presentation/ui/screens/AiConfigScreen.kt` | AI 配置 UI (604行) |
| SettingsViewModel | `presentation/viewmodel/SettingsViewModel.kt` | 配置管理状态 |
| AiModelConfigRepository | `data/repository/AiModelConfigRepository.kt` | 配置数据仓库 |
| AiModelConfigEntity | `data/local/entity/AiModelConfigEntity.kt` | 数据库实体 |
| AiModelConfigDao | `data/local/dao/AiModelConfigDao.kt` | Room DAO |
| AiServiceRepository | `data/repository/AiServiceRepository.kt` | API 调用调度（含端点路径映射） |
| AiApiService | `data/remote/AiApiService.kt` | Retrofit API 接口（@Url 动态端点） |
| AiApiClient | `data/remote/AiApiClient.kt` | OkHttp + Retrofit 客户端 |

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
    val temperature: Float = 0.7f,
    val maxTokens: Int = 50000,
    val topP: Float = 1.0f,
    val topK: Int = 50,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val thinkingEnabled: Boolean = true,       // 深度思考模式
    val reasoningEffort: String = "high",      // 推理力度 (high/max)
    val isDefault: Boolean = false   // 是否为默认配置
)
```

### 支持的服务商
```kotlin
// AiModelConfigRepository.kt
const val PROVIDER_OPENAI = "openai"
const val PROVIDER_ANTHROPIC = "anthropic"
const val PROVIDER_DEEPSEEK = "deepseek"
const val PROVIDER_ZHIPU = "zhipu"
const val PROVIDER_KIMI = "kimi"
const val PROVIDER_SILICONFLOW = "siliconflow"
const val PROVIDER_CUSTOM = "custom"

const val MODEL_GPT_35_TURBO = "gpt-3.5-turbo"
const val MODEL_GPT_4 = "gpt-4"
const val MODEL_CLAUDE_3 = "claude-3-opus"
const val MODEL_DEEPSEEK_CHAT = "deepseek-v4-flash"
const val MODEL_GLM4_FLASH = "glm-4-flash"
const val MODEL_MOONSHOT_V1_8K = "moonshot-v1-8k"
const val MODEL_DEEPSEEK_V3 = "deepseek-ai/DeepSeek-V3"
```

### 服务商默认 Base URL 与模型

| 服务商 | 默认模型 | 默认 Base URL |
|--------|----------|---------------|
| OpenAI | gpt-3.5-turbo | https://api.openai.com |
| Anthropic | claude-3-opus | https://api.anthropic.com |
| DeepSeek | deepseek-v4-flash | https://api.deepseek.com |
| 智谱 (GLM) | glm-4-flash | https://open.bigmodel.cn |
| Kimi (Moonshot) | moonshot-v1-8k | https://api.moonshot.cn |
| 硅基流动 | deepseek-ai/DeepSeek-V3 | https://api.siliconflow.cn |

切换服务商时，`SettingsViewModel.updateAiProvider()` 会自动填充对应的默认 Base URL 和模型名称。选择"自定义 API"时保留用户已输入的 Base URL 和模型名称不变。

### 表单验证
```kotlin
data class AiConfigFormData(
    val id: Int = 0,
    val configName: String = "",
    val provider: String = "deepseek",
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val modelName: String = "deepseek-v4-flash",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 50000,
    val topP: Float = 1.0f,
    val topK: Int = 50,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val thinkingEnabled: Boolean = true,
    val reasoningEffort: String = "high",
    val isDefault: Boolean = false,
    val configNameError: UiText? = null,   // 名称验证错误
    val apiKeyError: UiText? = null         // API Key 验证错误
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
4. Repository：检查配置名称唯一性（`getConfigByName()`）
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

### 参数输入
编辑页面使用 Material 3 组件：
- **Temperature**: `SettingsSliderItem` — 0.1 ~ 2.0，步长 0.1，默认 0.7
- **Max Tokens**: `SettingsIntEditItem` — `OutlinedTextField` + `KeyboardType.Number`，仅允许正整数输入，默认 50000
- **Top P**: `SettingsSliderItem` — 0.0 ~ 1.0，步长 0.1，默认 1.0
- **Top K**: `SettingsSliderItem` — 1 ~ 100，步长 1，默认 50
- **Frequency Penalty**: `SettingsSliderItem` — -2.0 ~ 2.0，步长 0.1，默认 0
- **Presence Penalty**: `SettingsSliderItem` — -2.0 ~ 2.0，步长 0.1，默认 0

### Custom 服务商
当 provider 选择 "Custom" 时，额外显示 Base URL 输入框（必填）。其他已知服务商会自动填充默认 Base URL，不显示此字段。

### DeepSeek 思考模式
当 provider 选择 "DeepSeek" 时，UI 额外显示思考模式配置：

- **thinkingEnabled** (`Boolean`, 默认 `true`)：通过 `Switch` 控件控制是否启用深度思考模式
- **reasoningEffort** (`String`, 默认 `"high"`)：当 thinkingEnabled 为 true 时显示，通过下拉菜单选择推理力度：
  - `"high"` — 高推理力度
  - `"max"` — 最大推理力度
- 当 thinkingEnabled 启用时，Temperature、Top P、Frequency Penalty、Presence Penalty 参数隐藏（DeepSeek 思考模式下这些参数不适用）
- ViewModel 函数：`updateAiThinkingEnabled(Boolean)`、`updateAiReasoningEffort(String)`
- 数据库字段：`thinking_enabled` (TEXT, `"true"`/`"false"`)、`reasoning_effort` (TEXT)

### 安全注意
API Key 当前以明文存储在 Room 数据库中，**未加密**。这是已知的安全风险。

## 已知问题/技术债务

1. **API Key 明文存储**：需要实现加密存储（如 EncryptedSharedPreferences 或 Android Keystore）
2. 缺少 API Key 有效性验证（可通过测试 API 调用实现）
3. ~~网络请求层尚未实现，AI 配置当前仅存储参数~~（已解决：`AiApiService.kt`、`AiApiClient.kt` 已实现）

### AI API 端点路径

`AiApiService` 使用 Retrofit `@Url` 参数实现动态端点路径。`AiServiceRepository.getChatCompletionsPath()` 根据 provider 返回对应的 API 路径：
- 大多数服务商使用 OpenaAI 兼容路径 `v1/chat/completions`
- 智谱 (GLM) 使用专属路径 `api/paas/v4/chat/completions`

---

**文档版本**: 1.2  
**最后更新**: 2026-05-10  
**变更**: 修正 temperature 默认值 (0.8→0.7)、topK 默认值 (1→50)、MODEL_DEEPSEEK_CHAT 值；新增 thinkingEnabled/reasoningEffort 字段；修正 AiConfigFormData 默认 provider/baseUrl/modelName；新增 DeepSeek 思考模式文档；新增 AiApiClient.kt 文件；更新已知问题#3 为已解决
