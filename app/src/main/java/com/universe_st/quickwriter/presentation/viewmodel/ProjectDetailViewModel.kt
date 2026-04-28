package com.universe_st.quickwriter.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.UiText
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

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
                    _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_project_not_found))
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_project_load_failed))
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
                        UiText.StringResource(R.string.error_save_cover_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_save_cover_failed))
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
                        UiText.StringResource(R.string.error_delete_cover_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_delete_cover_failed))
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
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_set_current_project_failed))
            }
        }
    }

    fun unsetCurrentProject() {
        viewModelScope.launch {
            try {
                settingsUseCase.setCurrentProjectId(null)
                _isCurrentProject.value = false
            } catch (e: Exception) {
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_cancel_current_project_failed))
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
                _uiState.value = ProjectDetailUiState.Error(UiText.StringResource(R.string.error_project_delete_failed))
            }
        }
    }

    fun performExport(context: Context, projectId: String, targetUri: android.net.Uri) {
        val tag = "ProjectDetailVM.performExport"
        Timber.tag(tag).d("Starting export: projectId=%s, uri=%s", projectId, targetUri)
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Exporting
            try {
                val tempFile = java.io.File(context.cacheDir, "${projectId}_export.zip")
                Timber.tag(tag).d("Temp file: %s", tempFile.absolutePath)
                try {
                    withContext(Dispatchers.IO) {
                        val zipResult = projectManagementUseCase.exportProjectAsZip(projectId, tempFile)
                        if (zipResult.isFailure) {
                            val ex = zipResult.exceptionOrNull()
                            Timber.tag(tag).e(ex, "ZIP creation failed")
                            throw java.io.IOException("ZIP creation failed", ex)
                        }
                        Timber.tag(tag).d("ZIP created: size=%d bytes", tempFile.length())

                        val output = context.contentResolver.openOutputStream(targetUri)
                        if (output == null) {
                            Timber.tag(tag).e("openOutputStream returned null for uri=%s", targetUri)
                            throw java.io.IOException("Cannot open output stream for target URI")
                        }
                        output.use { out ->
                            tempFile.inputStream().use { input ->
                                val copied = input.copyTo(out, 8192)
                                Timber.tag(tag).d("Copied %d bytes to target URI", copied)
                            }
                        }
                    }
                } finally {
                    withContext(Dispatchers.IO) {
                        val deleted = tempFile.delete()
                        Timber.tag(tag).d("Temp file deleted: %s", deleted)
                    }
                }
                _uiState.value = ProjectDetailUiState.ExportSuccess
                Timber.tag(tag).d("Export success")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Export failed: %s", e.message)
                _uiState.value = ProjectDetailUiState.ExportError(
                    UiText.DynamicString(e.message ?: "Unknown error")
                )
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
    data class Error(val message: UiText) : ProjectDetailUiState()
    object DeleteSuccess : ProjectDetailUiState()
    object SetCurrentSuccess : ProjectDetailUiState()
    object Exporting : ProjectDetailUiState()
    object ExportSuccess : ProjectDetailUiState()
    data class ExportError(val message: UiText) : ProjectDetailUiState()
}
