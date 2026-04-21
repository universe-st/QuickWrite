package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectListUiState>(ProjectListUiState.Loading)
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    private val _sortOption = MutableStateFlow(ProjectManagementUseCase.SortOption.MODIFIED_TIME)
    val sortOption: StateFlow<ProjectManagementUseCase.SortOption> = _sortOption.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.Loading
            try {
                projectManagementUseCase.getSortedProjects(_sortOption.value).collect { projects ->
                    if (projects.isEmpty()) {
                        _uiState.value = ProjectListUiState.Empty
                    } else {
                        _uiState.value = ProjectListUiState.Success(projects)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.Error(e.message ?: "加载项目失败")
            }
        }
    }

    fun sortProjects(sortOption: ProjectManagementUseCase.SortOption) {
        _sortOption.value = sortOption
        loadProjects()
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectListUiState.Loading
            try {
                projectManagementUseCase.deleteProject(projectId)
                loadProjects()
            } catch (e: Exception) {
                _uiState.value = ProjectListUiState.Error(e.message ?: "删除项目失败")
            }
        }
    }
}

class ProjectListViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectListViewModel(projectManagementUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class ProjectListUiState {
    object Loading : ProjectListUiState()
    object Empty : ProjectListUiState()
    data class Success(val projects: List<ProjectEntity>) : ProjectListUiState()
    data class Error(val message: String) : ProjectListUiState()
}