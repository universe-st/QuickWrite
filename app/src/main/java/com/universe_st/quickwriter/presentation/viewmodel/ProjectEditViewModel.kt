package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectEditViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectEditUiState>(ProjectEditUiState.Idle)
    val uiState: StateFlow<ProjectEditUiState> = _uiState.asStateFlow()

    private val _formData = MutableStateFlow(ProjectFormData())
    val formData: StateFlow<ProjectFormData> = _formData.asStateFlow()

    private var currentProjectId: String? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectEditUiState.Loading
            try {
                val project = projectManagementUseCase.getProjectById(projectId)
                if (project != null) {
                    currentProjectId = projectId
                    _formData.value = ProjectFormData(
                        title = project.title,
                        author = project.author,
                        genre = project.genre,
                        description = project.description ?: "",
                        coverImagePath = project.coverImagePath
                    )
                    _uiState.value = ProjectEditUiState.Success(project)
                } else {
                    _uiState.value = ProjectEditUiState.Error("项目不存在")
                }
            } catch (e: Exception) {
                _uiState.value = ProjectEditUiState.Error(e.message ?: "加载项目失败")
            }
        }
    }

    fun updateTitle(title: String) {
        _formData.value = _formData.value.copy(title = title)
        validateFormData()
    }

    fun updateAuthor(author: String) {
        _formData.value = _formData.value.copy(author = author)
        validateFormData()
    }

    fun updateGenre(genre: String) {
        _formData.value = _formData.value.copy(genre = genre)
    }

    fun updateDescription(description: String) {
        _formData.value = _formData.value.copy(description = description)
    }

    fun updateCoverImagePath(coverImagePath: String?) {
        _formData.value = _formData.value.copy(coverImagePath = coverImagePath)
    }

    private fun validateFormData() {
        viewModelScope.launch {
            val titleError = if (!isFormDataChanged()) {
                null
            } else {
                projectManagementUseCase.validateProjectTitle(_formData.value.title, currentProjectId)
                    .exceptionOrNull()?.message
            }

            val authorError = projectManagementUseCase.validateProjectAuthor(_formData.value.author)
                .exceptionOrNull()?.message

            _formData.value = _formData.value.copy(
                titleError = titleError,
                authorError = authorError
            )
        }
    }

    private fun isFormDataChanged(): Boolean {
        val currentState = _uiState.value
        if (currentState !is ProjectEditUiState.Success) return false
        val originalProject = currentState.project

        return _formData.value.title != originalProject.title ||
                _formData.value.author != originalProject.author ||
                _formData.value.genre != originalProject.genre ||
                _formData.value.description != (originalProject.description ?: "") ||
                _formData.value.coverImagePath != originalProject.coverImagePath
    }

    fun updateProject() {
        val projectId = currentProjectId
        if (projectId == null) {
            _uiState.value = ProjectEditUiState.Error("项目未加载")
            return
        }

        if (_formData.value.titleError != null || _formData.value.authorError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = ProjectEditUiState.Loading
            try {
                val result = projectManagementUseCase.updateProject(
                    id = projectId,
                    title = _formData.value.title,
                    author = _formData.value.author,
                    genre = _formData.value.genre,
                    description = _formData.value.description,
                    coverImagePath = _formData.value.coverImagePath
                )

                if (result.isSuccess) {
                    _uiState.value = ProjectEditUiState.UpdateSuccess(result.getOrNull()!!)
                } else {
                    _uiState.value = ProjectEditUiState.Error(
                        result.exceptionOrNull()?.message ?: "更新项目失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectEditUiState.Error(
                    e.message ?: "更新项目失败"
                )
            }
        }
    }

    fun resetForm() {
        if (currentProjectId != null) {
            loadProject(currentProjectId!!)
        }
        _uiState.value = ProjectEditUiState.Idle
    }
}

class ProjectEditViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectEditViewModel(projectManagementUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class ProjectEditUiState {
    object Idle : ProjectEditUiState()
    object Loading : ProjectEditUiState()
    data class Success(val project: ProjectEntity) : ProjectEditUiState()
    data class UpdateSuccess(val project: ProjectEntity) : ProjectEditUiState()
    data class Error(val message: String) : ProjectEditUiState()
}
