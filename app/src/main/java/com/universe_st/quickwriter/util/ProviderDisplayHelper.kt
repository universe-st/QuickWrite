package com.universe_st.quickwriter.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R

object ProviderDisplayHelper {

    private val providerMap = mapOf(
        "deepseek" to R.string.ai_provider_deepseek,
        "kimi" to R.string.ai_provider_kimi,
        "openai" to R.string.ai_provider_openai,
        "anthropic" to R.string.ai_provider_anthropic,
        "zhipu" to R.string.ai_provider_zhipu,
        "qwen" to R.string.ai_provider_qwen,
        "moonshot" to R.string.ai_provider_moonshot,
        "siliconflow" to R.string.ai_provider_siliconflow,
        "custom" to R.string.ai_provider_custom
    )

    @Composable
    fun getDisplayName(provider: String): String {
        val resId = providerMap[provider.lowercase()] ?: return provider
        return stringResource(resId)
    }

    fun getProviderList(): List<String> {
        return listOf("deepseek", "kimi", "openai", "zhipu", "qwen", "moonshot", "siliconflow", "custom")
    }
}
