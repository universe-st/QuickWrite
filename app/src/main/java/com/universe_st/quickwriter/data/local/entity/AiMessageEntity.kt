package com.universe_st.quickwriter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id", "created_at"], name = "idx_messages_session"),
        Index(value = ["session_id", "message_order"], name = "idx_messages_order")
    ]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "message_order")
    val messageOrder: Int,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "token_count")
    val tokenCount: Int = 0,
    @ColumnInfo(name = "tool_call_id")
    val toolCallId: String? = null,
    @ColumnInfo(name = "tool_calls_json")
    val toolCallsJson: String? = null,
    @ColumnInfo(name = "is_silent")
    val isSilent: Boolean = false,
    @ColumnInfo(name = "reasoning_content")
    val reasoningContent: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
