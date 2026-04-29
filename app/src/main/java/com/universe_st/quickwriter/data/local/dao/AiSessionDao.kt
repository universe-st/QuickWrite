package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universe_st.quickwriter.data.local.entity.AiSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiSessionDao {

    @Query("SELECT * FROM ai_sessions WHERE project_id = :projectId ORDER BY updated_at DESC")
    fun getSessionsByProject(projectId: String): Flow<List<AiSessionEntity>>

    @Query("SELECT * FROM ai_sessions WHERE project_id = :projectId ORDER BY updated_at DESC")
    suspend fun getSessionsByProjectDirect(projectId: String): List<AiSessionEntity>

    @Query("SELECT * FROM ai_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): AiSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiSessionEntity): Long

    @Update
    suspend fun updateSession(session: AiSessionEntity)

    @Query("DELETE FROM ai_sessions WHERE session_id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("UPDATE ai_sessions SET title = :title, updated_at = :updatedAt WHERE session_id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, title: String, updatedAt: Long)
}
