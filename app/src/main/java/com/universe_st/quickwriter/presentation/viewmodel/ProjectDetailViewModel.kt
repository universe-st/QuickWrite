package com.universe_st.quickwriter.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectDetailViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private val _isCurrentProject = MutableStateFlow(false)
    val isCurrentProject: StateFlow<Boolean> = _isCurrentProject.asStateFlow()

    private val _hasCoverImage = MutableStateFlow(false)
    val hasCoverImage: StateFlow<Boolean> = _hasCoverImage.asStateFlow()

    private val _coverImagePath = MutableStateFlow<String?>(null)
    val coverImagePath: StateFlow<String?> = _coverImagePath.asStateFlow()

    private var currentProject: ProjectEntity? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Loading
            try {
                val project = projectManagementUseCase.getProjectById(projectId)
                if (project != null) {
                    currentProject = project
                    _uiState.value = ProjectDetailUiState.Success(project)
                    refreshCoverState(projectId)

                    val currentProjectId = settingsUseCase.getCurrentProjectId()
                    _isCurrentProject.value = currentProjectId == projectId
                } else {
                    _uiState.value = ProjectDetailUiState.Error("项目不存在")
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(e.message ?: "加载项目失败")
            }
        }
    }

    private fun refreshCoverState(projectId: String) {
        val hasCover = projectManagementUseCase.hasCoverImage(projectId)
        _hasCoverImage.value = hasCover
        _coverImagePath.value = if (hasCover) {
            projectManagementUseCase.getCoverImagePath(projectId)
        } else {
            null
        }
    }

    fun saveCoverImage(context: Context, sourceUri: Uri, projectId: String) {
        viewModelScope.launch {
            try {
                val result = projectManagementUseCase.saveCoverImage(context, sourceUri, projectId)
                if (result.isSuccess) {
                    refreshCoverState(projectId)
                    reloadProject(projectId)
                } else {
                    _uiState.value = ProjectDetailUiState.Error(
                        result.exceptionOrNull()?.message ?: "保存书封失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(e.message ?: "保存书封失败")
            }
        }
    }

    fun deleteCoverImage(projectId: String) {
        viewModelScope.launch {
            try {
                val result = projectManagementUseCase.deleteCoverImage(projectId)
                if (result.isSuccess) {
                    refreshCoverState(projectId)
                    reloadProject(projectId)
                } else {
                    _uiState.value = ProjectDetailUiState.Error(
                        result.exceptionOrNull()?.message ?: "删除书封失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(e.message ?: "删除书封失败")
            }
        }
    }

    private fun reloadProject(projectId: String) {
        val project = currentProject ?: return
        viewModelScope.launch {
            try {
                val reloaded = projectManagementUseCase.getProjectById(projectId)
                if (reloaded != null) {
                    currentProject = reloaded
                    _uiState.value = ProjectDetailUiState.Success(reloaded)
                }
            } catch (_: Exception) {}
        }
    }

    fun setCurrentProject(projectId: String) {
        viewModelScope.launch {
            try {
                settingsUseCase.setCurrentProjectId(projectId)
                _isCurrentProject.value = true
                _uiState.value = ProjectDetailUiState.SetCurrentSuccess
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error("设置当前项目失败")
            }
        }
    }

    fun unsetCurrentProject() {
        viewModelScope.launch {
            try {
                settingsUseCase.setCurrentProjectId(null)
                _isCurrentProject.value = false
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error("取消当前项目失败")
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Loading
            try {
                projectManagementUseCase.deleteProject(projectId)

                if (_isCurrentProject.value) {
                    settingsUseCase.setCurrentProjectId(null)
                }

                _uiState.value = ProjectDetailUiState.DeleteSuccess
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(e.message ?: "删除项目失败")
            }
        }
    }
}

class ProjectDetailViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectDetailViewModel(projectManagementUseCase, settingsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class ProjectDetailUiState {
    object Loading : ProjectDetailUiState()
    data class Success(val project: ProjectEntity) : ProjectDetailUiState()
    data class Error(val message: String) : ProjectDetailUiState()
    object DeleteSuccess : ProjectDetailUiState()
    object SetCurrentSuccess : ProjectDetailUiState()
}
