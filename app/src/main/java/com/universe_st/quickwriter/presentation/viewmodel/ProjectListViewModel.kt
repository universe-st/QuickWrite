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
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectListUiState>(ProjectListUiState.Loading)
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    private val _sortOption = MutableStateFlow(ProjectManagementUseCase.SortOption.MODIFIED_TIME)
    val sortOption: StateFlow<ProjectManagementUseCase.SortOption> = _sortOption.asStateFlow()

    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    init {
        loadCurrentProjectId()
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.Loading
            try {
                combine(
                    _sortOption,
                    _currentProjectId
                ) { sortOption, currentProjectId ->
                    Pair(sortOption, currentProjectId)
                }.collectLatest { (sortOption, currentProjectId) ->
                    projectManagementUseCase.getSortedProjects(sortOption, currentProjectId).collect { projects ->
                        if (projects.isEmpty()) {
                            _uiState.value = ProjectListUiState.Empty
                        } else {
                            _uiState.value = ProjectListUiState.Success(projects)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.Error(UiText.StringResource(R.string.error_project_load_failed))
            }
        }
    }

    private fun loadCurrentProjectId() {
        viewModelScope.launch {
            settingsUseCase.getCurrentProjectIdAsFlow().collect { projectId ->
                _currentProjectId.value = projectId
            }
        }
    }

    fun sortProjects(sortOption: ProjectManagementUseCase.SortOption) {
        _sortOption.value = sortOption
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.Loading
            try {
                projectManagementUseCase.deleteProject(projectId)
                loadProjects()
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.Error(UiText.StringResource(R.string.error_project_delete_failed))
            }
        }
    }

    fun importProject(context: Context, zipUri: Uri) {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.Importing
            try {
                val result = projectManagementUseCase.importProjectFromZip(context, zipUri)
                if (result.isSuccess) {
                    _uiState.value = ProjectListUiState.ImportSuccess(
                        UiText.DynamicString(result.getOrThrow().title)
                    )
                    loadProjects()
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ProjectListUiState.ImportError(
                        UiText.DynamicString(error?.message ?: "Import failed")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.ImportError(
                    UiText.DynamicString(e.message ?: "Import failed")
                )
            }
        }
    }

    fun resetImportState() {
        val current = _uiState.value
        if (current is ProjectListUiState.ImportSuccess || current is ProjectListUiState.ImportError) {
            _uiState.value = ProjectListUiState.Empty
        }
    }
}

class ProjectListViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectListViewModel(projectManagementUseCase, settingsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class ProjectListUiState {
    object Loading : ProjectListUiState()
    object Empty : ProjectListUiState()
    object Importing : ProjectListUiState()
    data class Success(val projects: List<ProjectEntity>) : ProjectListUiState()
    data class Error(val message: UiText) : ProjectListUiState()
    data class ImportSuccess(val message: UiText) : ProjectListUiState()
    data class ImportError(val message: UiText) : ProjectListUiState()
}