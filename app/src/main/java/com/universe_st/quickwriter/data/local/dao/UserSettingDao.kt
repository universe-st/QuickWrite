package com.universe_st.quickwriter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.universe_st.quickwriter.data.local.entity.UserSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingDao {

    @Query("SELECT * FROM user_settings ORDER BY category, `key`")
    fun getAllSettings(): Flow<List<UserSettingEntity>>

    @Query("SELECT * FROM user_settings WHERE category = :category ORDER BY `key`")
    fun getSettingsByCategory(category: String): Flow<List<UserSettingEntity>>

    @Query("SELECT * FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): UserSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: UserSettingEntity)

    @Update
    suspend fun updateSetting(setting: UserSettingEntity)

    @Delete
    suspend fun deleteSetting(setting: UserSettingEntity)

    @Query("DELETE FROM user_settings WHERE `key` = :key")
    suspend fun deleteSettingByKey(key: String)

    @Query("DELETE FROM user_settings WHERE category = :category")
    suspend fun deleteSettingsByCategory(category: String)
}