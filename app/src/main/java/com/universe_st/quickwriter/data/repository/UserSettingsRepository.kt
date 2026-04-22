package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.UserSettingDao
import com.universe_st.quickwriter.data.local.entity.UserSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSettingsRepository(
    private val userSettingDao: UserSettingDao
) {

    companion object {
        const val THEME_KEY = "theme_mode"
        const val THEME_CATEGORY = "appearance"
        
        const val FONT_SIZE_KEY = "font_size"
        const val FONT_FAMILY_KEY = "font_family"
        const val FONT_CATEGORY = "appearance"
        
        const val AUTO_SAVE_INTERVAL_KEY = "auto_save_interval"
        const val AUTO_SAVE_IMMEDIATELY_KEY = "auto_save_immediately"
        const val DEFAULT_AUTO_SAVE_INTERVAL = "5"
        const val GENERAL_CATEGORY = "general"
        
        const val USE_MODEL_CONFIG_KEY = "use_model_config"
        const val DEFAULT_TEMPERATURE_KEY = "default_temperature"
        const val DEFAULT_MAX_TOKENS_KEY = "default_max_tokens"
        const val DEFAULT_TOP_P_KEY = "default_top_p"
        const val AI_WRITING_CATEGORY = "ai_writing"
        
        const val ENABLE_DARK_MODE_KEY = "enable_dark_mode"
        const val FOLLOW_SYSTEM_THEME_KEY = "follow_system_theme"
    }

    fun getAllSettings(): Flow<List<UserSettingEntity>> {
        return userSettingDao.getAllSettings()
    }

    fun getSettingsByCategory(category: String): Flow<List<UserSettingEntity>> {
        return userSettingDao.getSettingsByCategory(category)
    }

    suspend fun getSetting(key: String): String? {
        return userSettingDao.getSettingByKey(key)?.value
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return getSetting(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String, category: String): Result<Unit> {
        return try {
            val setting = UserSettingEntity(
                key = key,
                value = value,
                category = category
            )
            userSettingDao.insertSetting(setting)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSetting(key: String): Result<Unit> {
        return try {
            userSettingDao.deleteSettingByKey(key)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSettingsByCategory(category: String): Result<Unit> {
        return try {
            userSettingDao.deleteSettingsByCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getThemeMode(): String {
        return getSetting(THEME_KEY, "system")
    }

    suspend fun setThemeMode(mode: String): Result<Unit> {
        return setSetting(THEME_KEY, mode, THEME_CATEGORY)
    }

    suspend fun getFontSize(): Int {
        return getSetting(FONT_SIZE_KEY, "14").toIntOrNull() ?: 14
    }

    suspend fun setFontSize(size: Int): Result<Unit> {
        return setSetting(FONT_SIZE_KEY, size.toString(), FONT_FAMILY_KEY)
    }

    suspend fun getFontFamily(): String {
        return getSetting(FONT_FAMILY_KEY, "default")
    }

    suspend fun setFontFamily(family: String): Result<Unit> {
        return setSetting(FONT_FAMILY_KEY, family, FONT_FAMILY_KEY)
    }

    suspend fun getAutoSaveInterval(): Int {
        return getSetting(AUTO_SAVE_INTERVAL_KEY, DEFAULT_AUTO_SAVE_INTERVAL).toIntOrNull() ?: 5
    }

    suspend fun setAutoSaveInterval(minutes: Int): Result<Unit> {
        return setSetting(AUTO_SAVE_INTERVAL_KEY, minutes.toString(), GENERAL_CATEGORY)
    }

    suspend fun getAutoSaveImmediately(): Boolean {
        return getSetting(AUTO_SAVE_IMMEDIATELY_KEY, "false").toBooleanStrictOrNull() ?: false
    }

    suspend fun setAutoSaveImmediately(enabled: Boolean): Result<Unit> {
        return setSetting(AUTO_SAVE_IMMEDIATELY_KEY, enabled.toString(), GENERAL_CATEGORY)
    }

    suspend fun getDefaultTemperature(): Float {
        return getSetting(DEFAULT_TEMPERATURE_KEY, "0.8").toFloatOrNull() ?: 0.8f
    }

    suspend fun getUseModelConfig(): Boolean {
        return getSetting(USE_MODEL_CONFIG_KEY, "true").toBooleanStrictOrNull() ?: true
    }

    suspend fun setUseModelConfig(useModelConfig: Boolean): Result<Unit> {
        return setSetting(USE_MODEL_CONFIG_KEY, useModelConfig.toString(), AI_WRITING_CATEGORY)
    }

    suspend fun setDefaultTemperature(temperature: Float): Result<Unit> {
        return setSetting(DEFAULT_TEMPERATURE_KEY, temperature.toString(), AI_WRITING_CATEGORY)
    }

    suspend fun getDefaultMaxTokens(): Int {
        return getSetting(DEFAULT_MAX_TOKENS_KEY, "2000").toIntOrNull() ?: 2000
    }

    suspend fun setDefaultMaxTokens(tokens: Int): Result<Unit> {
        return setSetting(DEFAULT_MAX_TOKENS_KEY, tokens.toString(), AI_WRITING_CATEGORY)
    }

    suspend fun getDefaultTopP(): Float {
        return getSetting(DEFAULT_TOP_P_KEY, "1.0").toFloatOrNull() ?: 1.0f
    }

    suspend fun setDefaultTopP(topP: Float): Result<Unit> {
        return setSetting(DEFAULT_TOP_P_KEY, topP.toString(), AI_WRITING_CATEGORY)
    }
}