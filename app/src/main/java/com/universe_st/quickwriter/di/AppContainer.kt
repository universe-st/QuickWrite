package com.universe_st.quickwriter.di

import android.content.Context
import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.AiOperationDao
import com.universe_st.quickwriter.data.local.dao.AiSessionDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.dao.UserSettingDao
import com.universe_st.quickwriter.data.local.database.AppDatabase
import com.universe_st.quickwriter.data.remote.AiApiClient
import com.universe_st.quickwriter.data.remote.AppServiceContainer
import com.universe_st.quickwriter.data.repository.AiConversationRepository
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.data.repository.AiServiceRepository
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.data.repository.UserSettingsRepository
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
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

    val aiSessionDao: AiSessionDao by lazy {
        database.aiSessionDao()
    }

    val aiMessageDao: AiMessageDao by lazy {
        database.aiMessageDao()
    }

    val aiOperationDao: AiOperationDao by lazy {
        database.aiOperationDao()
    }

    val fileManager: FileManager by lazy {
        FileManager(context)
    }

    val aiApiClient: AiApiClient by lazy {
        AiApiClient()
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

    val aiServiceRepository: AiServiceRepository by lazy {
        AiServiceRepository(aiModelConfigDao, aiApiClient)
    }

    val aiConversationRepository: AiConversationRepository by lazy {
        AiConversationRepository(aiSessionDao, aiMessageDao)
    }

    val projectManagementUseCase: ProjectManagementUseCase by lazy {
        ProjectManagementUseCase(projectRepository, fileManager)
    }

    val settingsUseCase: SettingsUseCase by lazy {
        SettingsUseCase(userSettingsRepository, aiModelConfigRepository)
    }

    init {
        AppServiceContainer.aiSessionDao = aiSessionDao
        AppServiceContainer.aiMessageDao = aiMessageDao
        AppServiceContainer.aiOperationDao = aiOperationDao
        AppServiceContainer.aiServiceRepository = aiServiceRepository
        AppServiceContainer.projectDao = projectDao
        AppServiceContainer.aiModelConfigDao = aiModelConfigDao
        AppServiceContainer.fileManager = fileManager
        AppServiceContainer.projectRepository = projectRepository
        AppServiceContainer.projectManagementUseCase = projectManagementUseCase
    }
}
