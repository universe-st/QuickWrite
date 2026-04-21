package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.FileManager
import com.universe_st.quickwriter.util.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectCreateViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectCreateUiState>(ProjectCreateUiState.Idle)
    val uiState: StateFlow<ProjectCreateUiState> = _uiState.asStateFlow()

    private val _formData = MutableStateFlow(ProjectFormData())
    val formData: StateFlow<ProjectFormData> = _formData.asStateFlow()

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
            val titleError = projectManagementUseCase.validateProjectTitle(_formData.value.title)
                .exceptionOrNull()?.message

            val authorError = projectManagementUseCase.validateProjectAuthor(_formData.value.author)
                .exceptionOrNull()?.message

            _formData.value = _formData.value.copy(
                titleError = titleError,
                authorError = authorError
            )
        }
    }

    fun createProject() {
        if (_formData.value.titleError != null || _formData.value.authorError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = ProjectCreateUiState.Loading
            try {
                val result = projectManagementUseCase.createProject(
                    title = _formData.value.title,
                    author = _formData.value.author,
                    genre = _formData.value.genre,
                    description = _formData.value.description,
                    coverImagePath = _formData.value.coverImagePath
                )

                if (result.isSuccess) {
                    _uiState.value = ProjectCreateUiState.Success(result.getOrNull()!!)
                } else {
                    _uiState.value = ProjectCreateUiState.Error(
                        result.exceptionOrNull()?.message ?: "创建项目失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProjectCreateUiState.Error(
                    e.message ?: "创建项目失败"
                )
            }
        }
    }

    fun resetForm() {
        _formData.value = ProjectFormData()
        _uiState.value = ProjectCreateUiState.Idle
    }
}

class ProjectCreateViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectCreateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectCreateViewModel(projectManagementUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ProjectFormData(
    val title: String = "",
    val author: String = "",
    val genre: String = "玄幻",
    val description: String = "",
    val coverImagePath: String? = null,
    val titleError: String? = null,
    val authorError: String? = null
)

sealed class ProjectCreateUiState {
    object Idle : ProjectCreateUiState()
    object Loading : ProjectCreateUiState()
    data class Success(val project: ProjectEntity) : ProjectCreateUiState()
    data class Error(val message: String) : ProjectCreateUiState()
}