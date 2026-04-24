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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val extension: String = ""
)

data class Breadcrumb(val name: String, val path: String)

sealed class FileBrowserUiState {
    object Loading : FileBrowserUiState()
    data class Success(
        val project: ProjectEntity,
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>,
        val entries: List<FileEntry>,
        val selectedEntry: FileEntry?
    ) : FileBrowserUiState()
    data class Error(val message: String) : FileBrowserUiState()
}

class FileBrowserViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val projectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileBrowserUiState>(FileBrowserUiState.Loading)
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val _selectedEntry = MutableStateFlow<FileEntry?>(null)
    val selectedEntry: StateFlow<FileEntry?> = _selectedEntry.asStateFlow()

    private val _previewContent = MutableStateFlow<String?>(null)
    val previewContent: StateFlow<String?> = _previewContent.asStateFlow()

    private var project: ProjectEntity? = null

    fun loadProject() {
        viewModelScope.launch {
            _uiState.value = FileBrowserUiState.Loading
            try {
                val p = projectManagementUseCase.getProjectById(projectId)
                if (p == null) {
                    _uiState.value = FileBrowserUiState.Error("项目不存在")
                    return@launch
                }
                project = p
                navigateTo(p.storagePath)
            } catch (e: Exception) {
                _uiState.value = FileBrowserUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            val proj = project ?: return@launch
            try {
                val entriesResult = projectManagementUseCase.getDirectoryContents(path)
                if (entriesResult.isFailure) {
                    _uiState.value = FileBrowserUiState.Error("目录不存在: $path")
                    return@launch
                }
                val filePaths = entriesResult.getOrDefault(emptyList())
                val entries = filePaths.map { filePath ->
                    val file = File(filePath)
                    FileEntry(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0L,
                        lastModified = file.lastModified(),
                        extension = file.extension.lowercase()
                    )
                }.sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.name })

                val breadcrumbs = buildBreadcrumbs(path, proj.storagePath)

                _uiState.value = FileBrowserUiState.Success(
                    project = proj,
                    currentPath = path,
                    breadcrumbs = breadcrumbs,
                    entries = entries,
                    selectedEntry = _selectedEntry.value
                )
            } catch (e: Exception) {
                _uiState.value = FileBrowserUiState.Error(e.message ?: "加载目录失败")
            }
        }
    }

    fun navigateUp() {
        val state = _uiState.value as? FileBrowserUiState.Success ?: return
        val parent = File(state.currentPath).parent
        val proj = project ?: return
        if (parent != null && parent.startsWith(proj.storagePath)) {
            navigateTo(parent)
        }
    }

    fun enterDirectory(path: String) {
        navigateTo(path)
    }

    fun selectEntry(entry: FileEntry) {
        _selectedEntry.value = if (_selectedEntry.value?.path == entry.path) null else entry
    }

    fun previewFile(entry: FileEntry) {
        viewModelScope.launch {
            try {
                val content = projectManagementUseCase.readFileContent(entry.path)
                _previewContent.value = content.getOrDefault("").ifBlank { "(空文件)" }
            } catch (e: Exception) {
                _previewContent.value = "无法读取文件: ${e.message}"
            }
        }
    }

    fun clearPreview() {
        _previewContent.value = null
    }

    fun createFile(fileName: String) {
        val state = _uiState.value as? FileBrowserUiState.Success ?: return
        viewModelScope.launch {
            val filePath = File(state.currentPath, fileName).absolutePath
            val result = projectManagementUseCase.createFile(filePath)
            if (result.isFailure) {
                _uiState.value = FileBrowserUiState.Error(result.exceptionOrNull()?.message ?: "创建文件失败")
            } else {
                navigateTo(state.currentPath)
            }
        }
    }

    fun createDirectory(dirName: String) {
        val state = _uiState.value as? FileBrowserUiState.Success ?: return
        viewModelScope.launch {
            val dirPath = File(state.currentPath, dirName).absolutePath
            val result = projectManagementUseCase.createDirectory(dirPath)
            if (result.isFailure) {
                _uiState.value = FileBrowserUiState.Error(result.exceptionOrNull()?.message ?: "创建目录失败")
            } else {
                navigateTo(state.currentPath)
            }
        }
    }

    fun renameEntry(oldPath: String, newName: String) {
        val state = _uiState.value as? FileBrowserUiState.Success ?: return
        viewModelScope.launch {
            val oldFile = File(oldPath)
            val newPath = File(oldFile.parent, newName).absolutePath
            val result = projectManagementUseCase.renameFileOrDirectory(oldPath, newPath)
            if (result.isFailure) {
                _uiState.value = FileBrowserUiState.Error(result.exceptionOrNull()?.message ?: "重命名失败")
            } else {
                navigateTo(state.currentPath)
            }
        }
    }

    fun deleteEntry(path: String) {
        val state = _uiState.value as? FileBrowserUiState.Success ?: return
        viewModelScope.launch {
            val result = projectManagementUseCase.deleteFileOrDirectory(path)
            if (result.isFailure) {
                _uiState.value = FileBrowserUiState.Error(result.exceptionOrNull()?.message ?: "删除失败")
            } else {
                navigateTo(state.currentPath)
            }
        }
    }

    private fun buildBreadcrumbs(currentPath: String, rootPath: String): List<Breadcrumb> {
        val crumbs = mutableListOf(Breadcrumb("项目根目录", rootPath))
        val relative = currentPath.removePrefix(rootPath).trimStart(File.separatorChar)
        if (relative.isEmpty()) return crumbs

        val parts = relative.split(File.separatorChar)
        var accumulated = rootPath
        for (part in parts) {
            accumulated = File(accumulated, part).absolutePath
            crumbs.add(Breadcrumb(part, accumulated))
        }
        return crumbs
    }

    companion object {
        fun formatFileSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        }

        fun formatTimestamp(millis: Long): String {
            if (millis <= 0) return ""
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(millis))
        }
    }
}

class FileBrowserViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val projectId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileBrowserViewModel::class.java)) {
            return FileBrowserViewModel(projectManagementUseCase, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
