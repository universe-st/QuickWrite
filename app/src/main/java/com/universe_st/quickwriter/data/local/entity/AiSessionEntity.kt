package com.universe_st.quickwriter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_sessions",
    indices = [
        Index(value = ["project_id", "updated_at"], name = "idx_sessions_project"),
        Index(value = ["session_id"], unique = true, name = "idx_sessions_id")
    ]
)
data class AiSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "model_config_id")
    val modelConfigId: Int,
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
