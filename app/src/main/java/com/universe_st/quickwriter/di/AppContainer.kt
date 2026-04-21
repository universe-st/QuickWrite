package com.universe_st.quickwriter.di

import android.content.Context
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.dao.UserSettingDao
import com.universe_st.quickwriter.data.local.database.AppDatabase
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.data.repository.UserSettingsRepository
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val projectDao: ProjectDao by lazy {
        database.projectDao()
    }

    val aiModelConfigDao: AiModelConfigDao by lazy {
        database.aiModelConfigDao()
    }

    val userSettingDao: UserSettingDao by lazy {
        database.userSettingDao()
    }

    val fileManager: FileManager by lazy {
        FileManager(context)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(projectDao)
    }

    val aiModelConfigRepository: AiModelConfigRepository by lazy {
        AiModelConfigRepository(aiModelConfigDao)
    }

    val userSettingsRepository: UserSettingsRepository by lazy {
        UserSettingsRepository(userSettingDao)
    }

    val projectManagementUseCase: ProjectManagementUseCase by lazy {
        ProjectManagementUseCase(projectRepository, fileManager)
    }
}