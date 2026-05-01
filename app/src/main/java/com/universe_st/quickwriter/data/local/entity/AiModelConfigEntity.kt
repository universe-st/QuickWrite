package com.universe_st.quickwriter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_model_configs")
data class AiModelConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "config_name")
    val configName: String,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "api_key")
    val apiKey: String,
    @ColumnInfo(name = "base_url")
    val baseUrl: String?,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "temperature")
    val temperature: Float = 0.7f,
    @ColumnInfo(name = "max_tokens")
    val maxTokens: Int = 50000,
    @ColumnInfo(name = "top_p")
    val topP: Float = 1.0f,
    @ColumnInfo(name = "top_k")
    val topK: Int = 50,
    @ColumnInfo(name = "frequency_penalty")
    val frequencyPenalty: Float = 0.0f,
    @ColumnInfo(name = "presence_penalty")
    val presencePenalty: Float = 0.0f,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
)