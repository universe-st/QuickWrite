package com.universe_st.quickwriter.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id TEXT NOT NULL,
                    project_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    model_config_id INTEGER NOT NULL,
                    system_prompt TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_id ON ai_sessions (session_id)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_sessions_project ON ai_sessions (project_id, updated_at)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id TEXT NOT NULL,
                    message_order INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    token_count INTEGER NOT NULL DEFAULT 0,
                    tool_call_id TEXT,
                    tool_calls_json TEXT,
                    is_silent INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (session_id) REFERENCES ai_sessions(session_id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_messages_session ON ai_messages (session_id, created_at)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_messages_order ON ai_messages (session_id, message_order)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ai_operations (
                    id TEXT PRIMARY KEY NOT NULL,
                    operation_type TEXT NOT NULL,
                    project_id TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    tool_call_id TEXT NOT NULL,
                    file_path TEXT,
                    hash_before TEXT,
                    hash_after TEXT,
                    backup_file TEXT,
                    extra_data TEXT,
                    executed_at INTEGER NOT NULL,
                    FOREIGN KEY (session_id) REFERENCES ai_sessions(session_id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_ops_session ON ai_operations (session_id)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_ops_project ON ai_operations (project_id, executed_at)"
            )
        }
    }
}
