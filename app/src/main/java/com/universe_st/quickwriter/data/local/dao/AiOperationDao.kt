package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.universe_st.quickwriter.data.local.entity.AiOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiOperationDao {

    @Query("SELECT * FROM ai_operations WHERE session_id = :sessionId ORDER BY executed_at DESC")
    fun getOperationsBySession(sessionId: String): Flow<List<AiOperationEntity>>

    @Query("SELECT * FROM ai_operations WHERE project_id = :projectId ORDER BY executed_at DESC")
    fun getOperationsByProject(projectId: String): Flow<List<AiOperationEntity>>

    @Query("SELECT * FROM ai_operations WHERE id = :id")
    suspend fun getOperationById(id: String): AiOperationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: AiOperationEntity)

    @Query("DELETE FROM ai_operations WHERE id = :id")
    suspend fun deleteOperation(id: String)

    @Query("DELETE FROM ai_operations WHERE project_id = :projectId")
    suspend fun deleteOperationsByProject(projectId: String)
}
