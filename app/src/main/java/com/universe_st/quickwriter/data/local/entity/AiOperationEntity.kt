package com.universe_st.quickwriter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_operations",
    foreignKeys = [
        ForeignKey(
            entity = AiSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"], name = "idx_ops_session"),
        Index(value = ["project_id", "executed_at"], name = "idx_ops_project")
    ]
)
data class AiOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "operation_type")
    val operationType: String,
    @ColumnInfo(name = "project_id")
    val projectId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "tool_call_id")
    val toolCallId: String,
    @ColumnInfo(name = "file_path")
    val filePath: String?,
    @ColumnInfo(name = "hash_before")
    val hashBefore: String?,
    @ColumnInfo(name = "hash_after")
    val hashAfter: String?,
    @ColumnInfo(name = "backup_file")
    val backupFile: String?,
    @ColumnInfo(name = "extra_data")
    val extraData: String?,
    @ColumnInfo(name = "executed_at")
    val executedAt: Long
)
