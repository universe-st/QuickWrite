package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.UiText
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
import com.universe_st.quickwriter.util.AppUtils
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.ChapterMeta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChapterFileInfo(
    val fileName: String,
    val title: String,
    val order: Int,
    val volume: String,
    val summary: String,
    val filePath: String
)

sealed class WritingUiState {
    object NoProject : WritingUiState()
    object Loading : WritingUiState()
    data class Success(
        val project: ProjectEntity,
        val chapters: List<ChapterFileInfo>,
        val currentChapterIndex: Int,
        val editorContent: String,
        val currentChapterMeta: ChapterMeta,
        val wordCount: Int,
        val selectedTab: Int,
        val isSaving: Boolean,
        val isDirty: Boolean,
        val saveMessage: String? = null
    ) : WritingUiState()
    data class Error(val message: UiText) : WritingUiState()
}

class WritingViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WritingUiState>(WritingUiState.Loading)
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    private var saveMessageJob: Job? = null

    init {
        loadCurrentProject()
    }

    fun loadCurrentProject() {
        viewModelScope.launch {
            _uiState.value = WritingUiState.Loading
            try {
                val projectId = settingsUseCase.getCurrentProjectId()
                if (projectId == null) {
                    _uiState.value = WritingUiState.NoProject
                    return@launch
                }
                val project = projectManagementUseCase.getProjectById(projectId)
                if (project == null) {
                    _uiState.value = WritingUiState.NoProject
                    return@launch
                }
                loadChapters(project, useSavedTab = true)
            } catch (e: Exception) {
                _uiState.value = WritingUiState.Error(UiText.StringResource(R.string.error_project_load_failed))
            }
        }
    }

    private suspend fun loadChapters(project: ProjectEntity, useSavedTab: Boolean = false) {
        val chapterFilesResult = projectManagementUseCase.getChapterFiles(project.id)
        val filePaths = chapterFilesResult.getOrDefault(emptyList())

        val chapterInfoList = filePaths.mapNotNull { filePath ->
            val fileName = filePath.substringAfterLast('/')
            val content = projectManagementUseCase.readFileContent(filePath)
                .getOrDefault("")
            val (meta, _) = ChapterFileHelper.parseChapterContent(content)
            if (fileName.isBlank()) null
            else ChapterFileInfo(
                fileName = fileName,
                title = meta.title.ifBlank {
                    ChapterFileHelper.extractTitleFromBody(content)
                        .ifBlank { fileName.removeSuffix(".md") }
                },
                order = meta.order,
                volume = meta.volume,
                summary = meta.summary,
                filePath = filePath
            )
        }.sortedBy { it.order }

        val savedTab = if (useSavedTab && project.id == lastProjectId) lastSelectedTab else 0
        lastProjectId = project.id

        _uiState.value = WritingUiState.Success(
            project = project,
            chapters = chapterInfoList,
            currentChapterIndex = if (chapterInfoList.isEmpty()) -1 else 0,
            editorContent = "",
            currentChapterMeta = ChapterMeta(),
            wordCount = 0,
            selectedTab = savedTab,
            isSaving = false,
            isDirty = false
        )

        if (chapterInfoList.isNotEmpty()) {
            selectChapterInternal(project, chapterInfoList, 0)
        }
    }

    private suspend fun selectChapterInternal(
        project: ProjectEntity,
        chapters: List<ChapterFileInfo>,
        index: Int
    ) {
        if (index < 0 || index >= chapters.size) return
        val chapter = chapters[index]
        val content = projectManagementUseCase.readFileContent(chapter.filePath)
            .getOrDefault("")
        val (meta, body) = ChapterFileHelper.parseChapterContent(content)

        val updatedChapters = chapters.toMutableList()
        updatedChapters[index] = chapter.copy(
            title = meta.title.ifBlank { chapter.title },
            order = meta.order,
            volume = meta.volume,
            summary = meta.summary
        )

        val currentTab = (_uiState.value as? WritingUiState.Success)?.selectedTab ?: 0

        _uiState.value = WritingUiState.Success(
            project = project,
            chapters = updatedChapters,
            currentChapterIndex = index,
            editorContent = body,
            currentChapterMeta = meta,
            wordCount = countWords(body),
            selectedTab = currentTab,
            isSaving = false,
            isDirty = false
        )
    }

    fun selectChapter(index: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (index == state.currentChapterIndex) return
        saveCurrentChapter()
        viewModelScope.launch {
            selectChapterInternal(state.project, state.chapters, index)
        }
    }

    fun updateEditorContent(newContent: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (state.currentChapterIndex < 0) return
        _uiState.value = state.copy(
            editorContent = newContent,
            wordCount = countWords(newContent),
            isDirty = true,
            saveMessage = null
        )
    }

    fun saveCurrentChapter() {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val index = state.currentChapterIndex
        if (index < 0) return

        val chapter = state.chapters[index]
        val fullContent = ChapterFileHelper.buildChapterContent(
            state.currentChapterMeta, state.editorContent
        )
        if (!state.isDirty) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val result = projectManagementUseCase.writeFileContent(chapter.filePath, fullContent)
            if (result.isSuccess) {
                projectManagementUseCase.updateProjectStatistics(state.project.id)
                _uiState.value = state.copy(isSaving = false, isDirty = false, saveMessage = "已保存")
                clearSaveMessageAfterDelay()
            } else {
                _uiState.value = state.copy(
                    isSaving = false,
                    saveMessage = "保存失败"
                )
                clearSaveMessageAfterDelay()
            }
        }
    }

    private fun clearSaveMessageAfterDelay() {
        saveMessageJob?.cancel()
        saveMessageJob = viewModelScope.launch {
            delay(2000)
            val current = _uiState.value as? WritingUiState.Success
            if (current != null) {
                _uiState.value = current.copy(saveMessage = null)
            }
        }
    }

    fun createNewChapter(title: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        viewModelScope.launch {
            saveCurrentChapter()
            val sanitizedTitle = title.trim().ifBlank { "未命名章节" }
            val maxOrder = state.chapters.maxOfOrNull { it.order } ?: 0
            val fileName = "${sanitizedTitle}.md"
            val result = projectManagementUseCase.createChapterFile(state.project.id, fileName)
            if (result.isSuccess) {
                val filePath = result.getOrThrow()
                val meta = ChapterMeta(title = sanitizedTitle, order = maxOrder + 1)
                val body = "# $sanitizedTitle\n\n"
                val fullContent = ChapterFileHelper.buildChapterContent(meta, body)
                projectManagementUseCase.writeFileContent(filePath, fullContent)
                loadChapters(state.project)
            }
        }
    }

    fun deleteChapter(index: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (index < 0 || index >= state.chapters.size) return
        val chapter = state.chapters[index]
        viewModelScope.launch {
            projectManagementUseCase.deleteChapterFile(state.project.id, chapter.fileName)
            loadChapters(state.project)
        }
    }

    fun moveChapter(fromIndex: Int, toIndex: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val chapters = state.chapters
        if (fromIndex < 0 || fromIndex >= chapters.size ||
            toIndex < 0 || toIndex >= chapters.size
        ) return

        val from = chapters[fromIndex]
        val to = chapters[toIndex]

        viewModelScope.launch {
            val fromContent = projectManagementUseCase.readFileContent(from.filePath)
                .getOrDefault("")
            val (_, fromBody) = ChapterFileHelper.parseChapterContent(fromContent)
            val fromMeta = ChapterMeta(
                title = from.title,
                order = to.order,
                volume = from.volume,
                summary = from.summary
            )
            projectManagementUseCase.writeFileContent(
                from.filePath,
                ChapterFileHelper.buildChapterContent(fromMeta, fromBody)
            )

            val toContent = projectManagementUseCase.readFileContent(to.filePath)
                .getOrDefault("")
            val (_, toBody) = ChapterFileHelper.parseChapterContent(toContent)
            val toMeta = ChapterMeta(
                title = to.title,
                order = from.order,
                volume = to.volume,
                summary = to.summary
            )
            projectManagementUseCase.writeFileContent(
                to.filePath,
                ChapterFileHelper.buildChapterContent(toMeta, toBody)
            )

            loadChapters(state.project)
        }
    }

    fun setSelectedTab(tab: Int) {
        val state = _uiState.value
        if (state is WritingUiState.NoProject && tab == 0) return
        val success = state as? WritingUiState.Success ?: return
        _uiState.value = success.copy(selectedTab = tab)
        lastSelectedTab = tab
        lastProjectId = success.project.id
    }

    fun retry() {
        loadCurrentProject()
    }

    companion object {
        private var lastSelectedTab: Int = 0
        private var lastProjectId: String? = null
        fun countWords(text: String): Int {
            if (text.isBlank()) return 0
            val chinese = text.count { c ->
                c in '\u4E00'..'\u9FFF' ||
                    c in '\u3000'..'\u303F' ||
                    c in '\uFF00'..'\uFFEF'
            }
            val english = text.split(Regex("[\\s\\n]+"))
                .count { word ->
                    word.any { c ->
                        c in 'a'..'z' || c in 'A'..'Z'
                    }
                }
            return chinese + english
        }
    }
}

class WritingViewModelFactory(
    private val projectManagementUseCase: ProjectManagementUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WritingViewModel::class.java)) {
            return WritingViewModel(projectManagementUseCase, settingsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
