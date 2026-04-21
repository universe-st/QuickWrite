package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelConfigDao {

    @Query("SELECT * FROM ai_model_configs ORDER BY is_default DESC, id ASC")
    fun getAllConfigs(): Flow<List<AiModelConfigEntity>>

    @Query("SELECT * FROM ai_model_configs WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultConfig(): AiModelConfigEntity?

    @Query("SELECT * FROM ai_model_configs WHERE id = :id")
    suspend fun getConfigById(id: Int): AiModelConfigEntity?

    @Query("SELECT * FROM ai_model_configs WHERE config_name = :configName")
    suspend fun getConfigByName(configName: String): AiModelConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AiModelConfigEntity): Long

    @Update
    suspend fun updateConfig(config: AiModelConfigEntity)

    @Delete
    suspend fun deleteConfig(config: AiModelConfigEntity)

    @Query("UPDATE ai_model_configs SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultConfig(id: Int)

    @Query("UPDATE ai_model_configs SET is_default = 0")
    suspend fun clearDefaultConfig()
}