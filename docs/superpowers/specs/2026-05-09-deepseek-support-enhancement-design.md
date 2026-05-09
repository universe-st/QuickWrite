# DeepSeek Support Enhancement — Design Spec

**Date**: 2026-05-09 | **Status**: Approved

## Overview

Enhance QuickWrite to default to DeepSeek as the AI provider and add DeepSeek-specific configuration options: Thinking Mode (`thinking`) and Reasoning Effort (`reasoning_effort`).

## Requirements

1. Default AI provider: `deepseek` (was `openai`)
2. Default model: `deepseek-v4-flash` (was `gpt-3.5-turbo`)
3. When provider = `deepseek`, show:
   - **Thinking Mode** toggle (enabled/disabled), default = enabled
   - **Reasoning Effort** dropdown (high/max), default = high, only visible when thinking is enabled
4. When thinking mode is ON and provider = deepseek, hide/disable temperature, top_p, frequency_penalty, presence_penalty (per DeepSeek API: these have no effect in thinking mode)
5. Send `thinking` and `reasoning_effort` in API requests when applicable

## Changes by Layer

### Data Layer

**AiModelConfigEntity** — Add columns:
| Column | Type | Default |
|---|---|---|
| `thinking_enabled` | `Boolean` | `true` |
| `reasoning_effort` | `String` | `"high"` |

**AppDatabase** — Version 4 → 5, with `MIGRATION_4_5`.

### Repository Layer

**AiModelConfigRepository**: Update `MODEL_DEEPSEEK_CHAT` from `"deepseek-chat"` → `"deepseek-v4-flash"`.

### Presentation Layer

**AiConfigFormData** (SettingsViewModel):
- `provider` default: `"openai"` → `"deepseek"`
- `modelName` default: `"gpt-3.5-turbo"` → `"deepseek-v4-flash"`
- Add `thinkingEnabled: Boolean = true`
- Add `reasoningEffort: String = "high"`

**updateAiProvider()**: When switching to `deepseek`, auto-fill modelName to `"deepseek-v4-flash"`.

**AiConfigScreen**: When `provider == "deepseek"`:
- Show Thinking Mode switch
- Show Reasoning Effort dropdown (only when thinking is ON)
- Hide/disable: temperature, top_p, frequency_penalty, presence_penalty (when thinking is ON)

### API Layer

**AiModels.kt** — New DTO:
```kotlin
data class ThinkingConfig(val type: String) // "enabled" | "disabled"
```

**ChatCompletionRequest** — Add optional fields:
- `thinking: ThinkingConfig?`
- `reasoning_effort: String?`

**ApiDispatcher** — Include fields when provider = `deepseek` and config has them set.

### String Resources

Add 6 new strings (×3 languages):
| Key | EN | zh-CN | zh-TW |
|---|---|---|---|
| `ai_config_param_thinking` | Thinking Mode | 思考模式 | 思考模式 |
| `ai_config_param_thinking_desc` | DeepSeek thinking mode... | DeepSeek思考模式... | DeepSeek思考模式... |
| `ai_config_param_thinking_enabled` | Enabled | 开启 | 開啟 |
| `ai_config_param_thinking_disabled` | Disabled | 关闭 | 關閉 |
| `ai_config_param_reasoning_effort` | Reasoning Effort | 推理强度 | 推理強度 |
| `ai_config_param_reasoning_effort_desc` | Controls DepthSeek reasoning effort... | 控制DeepSeek推理强度... | 控制DeepSeek推理強度... |

### Migration

`MIGRATION_4_5`: `ALTER TABLE ai_model_configs ADD COLUMN thinking_enabled INTEGER NOT NULL DEFAULT 1` + `ADD COLUMN reasoning_effort TEXT NOT NULL DEFAULT 'high'`.

## Validation

- When provider is not deepseek, thinking/reasoning_effort controls are hidden and not included in API requests
- When thinking is OFF, reasoning_effort dropdown is hidden
- When thinking is ON + deepseek, temperature/top_p/frequency/presence sliders are hidden/disabled
- `reasoning_effort` is only valid as `"high"` or `"max"`

## Files Modified

| File | Change |
|---|---|
| `data/local/entity/AiModelConfigEntity.kt` | +2 columns |
| `data/local/database/AppDatabase.kt` | v4→v5, add migration |
| `data/local/database/Migrations.kt` | MIGRATION_4_5 |
| `data/repository/AiModelConfigRepository.kt` | Update MODEL_DEEPSEEK_CHAT |
| `presentation/viewmodel/SettingsViewModel.kt` | Defaults + new fields + update methods |
| `presentation/ui/screens/AiConfigScreen.kt` | Thinking/reasoning UI + conditional hiding |
| `data/remote/dto/AiModels.kt` | ThinkingConfig + ChatCompletionRequest fields |
| `data/remote/ApiDispatcher.kt` | Include new fields in request |
| `domain/usecase/SettingsUseCase.kt` | Pass new fields through |
| `res/values/strings.xml` | +6 strings |
| `res/values-zh-rCN/strings.xml` | +6 strings |
| `res/values-zh-rTW/strings.xml` | +6 strings |
