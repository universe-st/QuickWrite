package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import java.io.File
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.util.UiText
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
import com.universe_st.quickwriter.util.AppUtils
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.ChapterMeta
import com.universe_st.quickwriter.util.FileTreeItem
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

data class ReferenceBlock(
    val id: String,
    val filePath: String,
    val contentPreview: String,
    val content: String,
    val startLine: Int,
    val endLine: Int
) {
    companion object {
        fun buildReferenceText(blocks: List<ReferenceBlock>, userInput: String): String {
            if (blocks.isEmpty()) return userInput
            val refLines = blocks.joinToString("\n\n") { ref ->
                val lineInfo = if (ref.startLine == ref.endLine)
                    ref.startLine.toString()
                else
                    "${ref.startLine}-${ref.endLine}"
                val header = "<ref path=\"${ref.filePath}\" lines=\"$lineInfo\">"
                if (ref.content.lines().size < 5 && ref.content.isNotBlank()) {
                    "$header\n${ref.content}\n</ref>"
                } else {
                    "$header\n</ref>"
                }
            }
            return "$refLines\n\n$userInput"
        }
    }
}

enum class FileBrowserMode {
    CHAPTERS, SETTINGS, TIMELINE, LOGS, CONFIG;

    fun displayNameResId(): Int = when (this) {
        CHAPTERS -> R.string.writing_browse_chapters
        SETTINGS -> R.string.writing_browse_settings
        TIMELINE -> R.string.writing_browse_timeline
        LOGS -> R.string.writing_browse_logs
        CONFIG -> R.string.writing_browse_config
    }

    fun dirName(): String = when (this) {
        CHAPTERS -> "正文"
        SETTINGS -> "设定"
        TIMELINE -> "时间线"
        LOGS -> "记录"
        CONFIG -> "配置"
    }
}

sealed class WritingUiState {
    object NoProject : WritingUiState()
    object Loading : WritingUiState()
    data class Initializing(val current: Int, val total: Int) : WritingUiState()
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
        val autoSaveImmediately: Boolean = false,
        val saveMessage: String? = null,
        val fileBrowserMode: FileBrowserMode = FileBrowserMode.CHAPTERS,
        val fileTree: List<FileTreeItem> = emptyList(),
        val expandedFolders: Set<String> = emptySet(),
        val currentFilePath: String? = null,
        val fileLastModified: Long = 0,
        val editorScrollY: Int = 0,
        val editorSelectionStart: Int = 0,
        val referenceBlocks: List<ReferenceBlock> = emptyList()
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
    private var autoSaveTimerJob: Job? = null
    private var instantSaveJob: Job? = null

    private var autoSaveImmediately: Boolean = false
    private var autoSaveInterval: Int = 5

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
                loadAutoSaveSettings()
                val isSameProject = project.id == lastProjectId
                val savedChapterIdx = if (isSameProject) lastChapterIndex else 0
                val savedMode = if (isSameProject) lastFileBrowserMode else FileBrowserMode.CHAPTERS
                val savedFilePath = if (isSameProject) lastCurrentFilePath else null

                loadChapters(project, useSavedTab = true, restoreChapterIndex = savedChapterIdx)

                if (isSameProject && savedMode != FileBrowserMode.CHAPTERS) {
                    val state = _uiState.value as? WritingUiState.Success ?: return@launch
                    val dirPath = File(state.project.storagePath, savedMode.dirName()).absolutePath
                    val tree = projectManagementUseCase.getFileTree(dirPath)
                    var newState = state.copy(
                        fileBrowserMode = savedMode,
                        fileTree = tree,
                        currentFilePath = null,
                        editorContent = ""
                    )
                    if (savedFilePath != null) {
                        val file = File(savedFilePath)
                        if (file.exists()) {
                            val content = projectManagementUseCase.readFileContent(savedFilePath).getOrDefault("")
                            newState = newState.copy(
                                currentFilePath = savedFilePath,
                                editorContent = content,
                                wordCount = countWords(content),
                                fileLastModified = file.lastModified()
                            )
                        }
                    }
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = WritingUiState.Error(UiText.StringResource(R.string.error_project_load_failed))
            }
        }
    }

    private suspend fun loadAutoSaveSettings() {
        autoSaveImmediately = settingsUseCase.getAutoSaveImmediately()
        autoSaveInterval = settingsUseCase.getAutoSaveInterval()
    }

    private fun startAutoSaveIfNeeded() {
        stopAutoSaveTimer()
        if (autoSaveImmediately) {
            startInstantAutoSave()
        } else if (autoSaveInterval > 0) {
            startIntervalAutoSave()
        }
    }

    private fun startIntervalAutoSave() {
        stopAutoSaveTimer()
        autoSaveTimerJob = viewModelScope.launch {
            while (true) {
                delay(autoSaveInterval * 60 * 1000L)
                val state = _uiState.value as? WritingUiState.Success ?: continue
                if (state.isDirty) {
                    autoSaveIfNeeded(state)
                }
            }
        }
    }

    private fun startInstantAutoSave() {
        stopAutoSaveTimer()
    }

    private fun scheduleInstantSave() {
        if (!autoSaveImmediately) return
        instantSaveJob?.cancel()
        instantSaveJob = viewModelScope.launch {
            delay(1500)
            val state = _uiState.value as? WritingUiState.Success ?: return@launch
            if (state.isDirty) {
                autoSaveIfNeeded(state)
            }
        }
    }

    private fun stopAutoSaveTimer() {
        autoSaveTimerJob?.cancel()
        autoSaveTimerJob = null
        instantSaveJob?.cancel()
        instantSaveJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoSaveTimer()
        saveMessageJob?.cancel()
    }

    private suspend fun loadChapters(
        project: ProjectEntity,
        useSavedTab: Boolean = false,
        preserveIndex: Boolean = false,
        emitEmptyState: Boolean = true,
        restoreChapterIndex: Int = 0
    ) {
        val prevSuccess = _uiState.value as? WritingUiState.Success
        val prevScrollY = prevSuccess?.editorScrollY
            ?: if (project.id == lastProjectId) lastScrollY else 0
        val prevSelectionStart = prevSuccess?.editorSelectionStart
            ?: if (project.id == lastProjectId) lastSelectionStart else 0

        val currentFileName = if (preserveIndex) {
            val st = _uiState.value as? WritingUiState.Success
            st?.chapters?.getOrNull(st.currentChapterIndex)?.fileName
        } else null

        val chapterFilesResult = projectManagementUseCase.getChapterFiles(project.id)
        val filePaths = chapterFilesResult.getOrDefault(emptyList())

        val chaptersWithoutOrder = mutableListOf<Pair<String, String>>()
        for (filePath in filePaths) {
            val content = projectManagementUseCase.readFileContent(filePath).getOrDefault("")
            val (meta, _) = ChapterFileHelper.parseChapterContent(content)
            if (meta.order <= 0) {
                chaptersWithoutOrder.add(filePath to content)
            }
        }

        if (chaptersWithoutOrder.isNotEmpty()) {
            val existingOrders = filePaths.mapNotNull { fp ->
                if (chaptersWithoutOrder.any { it.first == fp }) null
                else {
                    val c = projectManagementUseCase.readFileContent(fp).getOrDefault("")
                    ChapterFileHelper.parseChapterContent(c).first.order
                }
            }.filter { it > 0 }
            var nextOrder = (existingOrders.maxOrNull() ?: 0) + 1

            val total = chaptersWithoutOrder.size
            chaptersWithoutOrder.forEachIndexed { index, (filePath, content) ->
                _uiState.value = WritingUiState.Initializing(current = index + 1, total = total)

                val (originalMeta, body) = ChapterFileHelper.parseChapterContent(content)
                val fileName = filePath.substringAfterLast('/')
                val title = if (originalMeta.title.isNotBlank()) originalMeta.title
                else ChapterFileHelper.extractTitleFromBody(body)
                    .ifBlank { fileName.removeSuffix(".md") }
                val meta = ChapterMeta(
                    title = title,
                    order = nextOrder++,
                    volume = originalMeta.volume,
                    summary = originalMeta.summary
                )
                val newContent = ChapterFileHelper.buildChapterContent(meta, body)
                projectManagementUseCase.writeFileContent(filePath, newContent)
            }
        }

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

        val targetIndex = if (preserveIndex && currentFileName != null) {
            chapterInfoList.indexOfFirst { it.fileName == currentFileName }.let { if (it >= 0) it else 0 }
        } else {
            restoreChapterIndex.coerceIn(0, (chapterInfoList.size - 1).coerceAtLeast(0))
        }

        if (emitEmptyState) {
            _uiState.value = WritingUiState.Success(
                project = project,
                chapters = chapterInfoList,
                currentChapterIndex = if (chapterInfoList.isEmpty()) -1 else targetIndex,
                editorContent = "",
                currentChapterMeta = ChapterMeta(),
                wordCount = 0,
                selectedTab = savedTab,
                isSaving = false,
                isDirty = false,
                autoSaveImmediately = autoSaveImmediately,
                editorScrollY = prevScrollY,
                editorSelectionStart = prevSelectionStart,
                referenceBlocks = prevSuccess?.referenceBlocks ?: emptyList()
            )

            if (chapterInfoList.isNotEmpty()) {
                selectChapterInternal(project, chapterInfoList, targetIndex)
            }
        } else {
            var editorContent = ""
            var chapterMeta = ChapterMeta()
            var fileLastModified = 0L
            var updatedChapters = chapterInfoList

            if (chapterInfoList.isNotEmpty() && targetIndex >= 0) {
                val chapter = chapterInfoList[targetIndex]
                val content = projectManagementUseCase.readFileContent(chapter.filePath).getOrDefault("")
                val (meta, body) = ChapterFileHelper.parseChapterContent(content)
                editorContent = body
                chapterMeta = meta
                val file = File(chapter.filePath)
                fileLastModified = if (file.exists()) file.lastModified() else 0L
                updatedChapters = chapterInfoList.toMutableList().apply {
                    this[targetIndex] = chapter.copy(
                        title = meta.title.ifBlank { chapter.title },
                        order = meta.order,
                        volume = meta.volume,
                        summary = meta.summary
                    )
                }
            }

            _uiState.value = WritingUiState.Success(
                project = project,
                chapters = updatedChapters,
                currentChapterIndex = if (chapterInfoList.isEmpty()) -1 else targetIndex,
                editorContent = editorContent,
                currentChapterMeta = chapterMeta,
                wordCount = countWords(editorContent),
                selectedTab = savedTab,
                isSaving = false,
                isDirty = false,
                autoSaveImmediately = autoSaveImmediately,
                fileLastModified = fileLastModified,
                editorScrollY = prevScrollY,
                editorSelectionStart = prevSelectionStart,
                referenceBlocks = prevSuccess?.referenceBlocks ?: emptyList()
            )
            lastChapterIndex = targetIndex.coerceIn(0, (chapterInfoList.size - 1).coerceAtLeast(0))
        }

        projectManagementUseCase.recalculateProjectWordCount(project.id)
        startAutoSaveIfNeeded()
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

        val currentState = _uiState.value as? WritingUiState.Success
        val currentTab = currentState?.selectedTab ?: 0
        val currentScrollY = currentState?.editorScrollY ?: 0
        val currentSelStart = currentState?.editorSelectionStart ?: 0
        val file = File(chapter.filePath)
        val fileLastModified = if (file.exists()) file.lastModified() else 0L

        _uiState.value = WritingUiState.Success(
            project = project,
            chapters = updatedChapters,
            currentChapterIndex = index,
            editorContent = body,
            currentChapterMeta = meta,
            wordCount = countWords(body),
            selectedTab = currentTab,
            isSaving = false,
            isDirty = false,
            autoSaveImmediately = autoSaveImmediately,
            fileLastModified = fileLastModified,
            editorScrollY = currentScrollY,
            editorSelectionStart = currentSelStart,
            referenceBlocks = currentState?.referenceBlocks ?: emptyList()
        )
        lastChapterIndex = index
        lastCurrentFilePath = null
    }

    fun selectChapter(index: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (index == state.currentChapterIndex) return
        instantSaveJob?.cancel()
        saveMessageJob?.cancel()
        viewModelScope.launch {
            saveCurrentChapterSuspend(state)
            selectChapterInternal(state.project, state.chapters, index)
        }
    }

    fun updateEditorContent(newContent: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (state.currentChapterIndex < 0 && state.currentFilePath == null) return
        _uiState.value = state.copy(
            editorContent = newContent,
            wordCount = countWords(newContent),
            isDirty = true,
            saveMessage = null
        )
        scheduleInstantSave()
    }

    fun saveCurrentChapter() {
        val state = _uiState.value as? WritingUiState.Success ?: return
        saveCurrentChapterInternal(state)
    }

    private suspend fun saveCurrentChapterSuspend(state: WritingUiState.Success): Boolean {
        val index = state.currentChapterIndex
        if (index < 0 || !state.isDirty) return false
        val chapter = state.chapters[index]

        val file = File(chapter.filePath)
        if (file.exists() && file.lastModified() > state.fileLastModified) {
            val content = projectManagementUseCase.readFileContent(chapter.filePath).getOrDefault("")
            val (meta, body) = ChapterFileHelper.parseChapterContent(content)
            val updatedChapters = state.chapters.toMutableList()
            updatedChapters[index] = chapter.copy(
                title = meta.title.ifBlank { chapter.title },
                order = meta.order,
                volume = meta.volume,
                summary = meta.summary
            )
            _uiState.value = state.copy(
                chapters = updatedChapters,
                editorContent = body,
                currentChapterMeta = meta,
                wordCount = countWords(body),
                isDirty = false,
                fileLastModified = file.lastModified()
            )
            return false
        }

        val fullContent = ChapterFileHelper.buildChapterContent(
            state.currentChapterMeta, state.editorContent
        )
        val result = projectManagementUseCase.writeFileContent(chapter.filePath, fullContent)
        if (result.isSuccess) {
            val newTimestamp = File(chapter.filePath).lastModified()
            val currentState = _uiState.value as? WritingUiState.Success
            if (currentState != null) {
                _uiState.value = currentState.copy(fileLastModified = newTimestamp)
            }
            projectManagementUseCase.updateProjectStatistics(state.project.id)
        }
        return result.isSuccess
    }

    private fun autoSaveIfNeeded(state: WritingUiState.Success) {
        if (!state.isDirty) return
        if (state.fileBrowserMode == FileBrowserMode.CHAPTERS) {
            saveCurrentChapterInternal(state)
        } else {
            saveCurrentFileInternal(state)
        }
    }

    private fun saveCurrentFileInternal(state: WritingUiState.Success) {
        val filePath = state.currentFilePath ?: return
        val savedEditorContent = state.editorContent
        viewModelScope.launch {
            val file = File(filePath)
            if (file.exists() && file.lastModified() > state.fileLastModified) {
                val content = projectManagementUseCase.readFileContent(filePath).getOrDefault("")
                val successState = (_uiState.value as? WritingUiState.Success)
                    ?: return@launch
                _uiState.value = successState.copy(
                    editorContent = content,
                    wordCount = countWords(content),
                    isDirty = false,
                    fileLastModified = file.lastModified()
                )
                return@launch
            }
            _uiState.value = (_uiState.value as? WritingUiState.Success)?.copy(isSaving = true) ?: return@launch
            val result = projectManagementUseCase.writeFileContent(filePath, savedEditorContent)
            val current = _uiState.value as? WritingUiState.Success ?: return@launch
            if (result.isSuccess) {
                val stillDirty = current.editorContent != savedEditorContent
                val newTimestamp = File(filePath).lastModified()
                _uiState.value = current.copy(
                    isSaving = false,
                    isDirty = stillDirty,
                    saveMessage = "已保存",
                    fileLastModified = newTimestamp
                )
                clearSaveMessageAfterDelay()
            } else {
                _uiState.value = current.copy(isSaving = false, saveMessage = "保存失败")
                clearSaveMessageAfterDelay()
            }
        }
    }

    private fun saveCurrentChapterInternal(state: WritingUiState.Success) {
        if (state.currentChapterIndex < 0 || !state.isDirty) return
        if (state.fileBrowserMode != FileBrowserMode.CHAPTERS) return
        val savedEditorContent = state.editorContent

        viewModelScope.launch {
            _uiState.value = (_uiState.value as? WritingUiState.Success)?.copy(isSaving = true) ?: return@launch
            val success = saveCurrentChapterSuspend(state)
            val current = _uiState.value as? WritingUiState.Success ?: return@launch
            if (success) {
                val stillDirty = current.editorContent != savedEditorContent
                _uiState.value = current.copy(isSaving = false, isDirty = stillDirty, saveMessage = "已保存")
                clearSaveMessageAfterDelay()
            } else if (current.isDirty) {
                _uiState.value = current.copy(isSaving = false, saveMessage = "保存失败")
                clearSaveMessageAfterDelay()
            } else {
                _uiState.value = current.copy(isSaving = false)
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
                projectManagementUseCase.recalculateProjectWordCount(state.project.id)
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
            projectManagementUseCase.recalculateProjectWordCount(state.project.id)
            loadChapters(state.project)
        }
    }

    fun switchBrowseMode(mode: FileBrowserMode) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (state.fileBrowserMode == mode) return
        viewModelScope.launch {
            if (mode == FileBrowserMode.CHAPTERS) {
                loadChapters(state.project)
            } else {
                val dirPath = File(state.project.storagePath, mode.dirName()).absolutePath
                val tree = projectManagementUseCase.getFileTree(dirPath)
                _uiState.value = state.copy(
                    fileBrowserMode = mode,
                    fileTree = tree,
                    currentFilePath = null,
                    editorContent = ""
                )
            }
            lastFileBrowserMode = mode
        }
    }

    fun toggleFolderExpanded(relativePath: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val expanded = state.expandedFolders.toMutableSet()
        if (expanded.contains(relativePath)) expanded.remove(relativePath)
        else expanded.add(relativePath)
        _uiState.value = state.copy(expandedFolders = expanded)
    }

    fun selectNonChapterFile(file: FileTreeItem) {
        if (file.isDirectory) {
            toggleFolderExpanded(file.relativePath)
            return
        }
        val state = _uiState.value as? WritingUiState.Success ?: return
        viewModelScope.launch {
            val content = projectManagementUseCase.readFileContent(file.absolutePath)
                .getOrDefault("")
            val diskFile = File(file.absolutePath)
            val lastMod = if (diskFile.exists()) diskFile.lastModified() else 0L
            _uiState.value = state.copy(
                currentFilePath = file.absolutePath,
                editorContent = content,
                wordCount = countWords(content),
                isDirty = false,
                saveMessage = null,
                fileLastModified = lastMod
            )
            lastCurrentFilePath = file.absolutePath
        }
    }

    fun saveCurrentFile() {
        val state = _uiState.value as? WritingUiState.Success ?: return
        saveCurrentFileInternal(state)
    }

    fun deleteFileOrFolder(item: FileTreeItem) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        viewModelScope.launch {
            projectManagementUseCase.deleteFileOrDir(item.absolutePath)
            refreshCurrentFileTree(state)
        }
    }

    fun deleteChapterWithConfirm(index: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (index < 0 || index >= state.chapters.size) return
        val chapter = state.chapters[index]
        viewModelScope.launch {
            projectManagementUseCase.deleteChapterFile(state.project.id, chapter.fileName)
            projectManagementUseCase.recalculateProjectWordCount(state.project.id)
            loadChapters(state.project)
        }
    }

    fun renameFile(oldPath: String, newName: String, isChapter: Boolean, chapterIndex: Int = -1) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        viewModelScope.launch {
            val result = projectManagementUseCase.renameFileOrDir(oldPath, newName)
            if (result.isSuccess) {
                if (isChapter) {
                    loadChapters(state.project)
                } else {
                    refreshCurrentFileTree(state)
                }
            }
        }
    }

    fun createNewFileInCurrentDir(fileName: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val dirPath = File(state.project.storagePath, state.fileBrowserMode.dirName()).absolutePath
        val filePath = File(dirPath, fileName).absolutePath
        viewModelScope.launch {
            projectManagementUseCase.createFileInProject(filePath)
            refreshCurrentFileTree(state)
        }
    }

    fun createNewFolderInCurrentDir(folderName: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val dirPath = File(state.project.storagePath, state.fileBrowserMode.dirName()).absolutePath
        val folderPath = File(dirPath, folderName).absolutePath
        viewModelScope.launch {
            projectManagementUseCase.createDirectoryInProject(folderPath)
            refreshCurrentFileTree(state)
        }
    }

    fun editChapterMeta(chapterIndex: Int, meta: ChapterMeta) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        if (chapterIndex < 0 || chapterIndex >= state.chapters.size) return
        val chapter = state.chapters[chapterIndex]
        viewModelScope.launch {
            val content = projectManagementUseCase.readFileContent(chapter.filePath)
                .getOrDefault("")
            val (_, body) = ChapterFileHelper.parseChapterContent(content)
            val newContent = ChapterFileHelper.buildChapterContent(meta, body)
            projectManagementUseCase.writeFileContent(chapter.filePath, newContent)
            loadChapters(state.project)
        }
    }

    private suspend fun refreshCurrentFileTree(state: WritingUiState.Success) {
        val dirPath = File(state.project.storagePath, state.fileBrowserMode.dirName()).absolutePath
        val tree = projectManagementUseCase.getFileTree(dirPath)
        _uiState.value = state.copy(fileTree = tree)
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
        if (tab == success.selectedTab) return
        _uiState.value = success.copy(selectedTab = tab)
        lastSelectedTab = tab
        lastProjectId = success.project.id

        if (tab == 0) {
            viewModelScope.launch {
                if (success.fileBrowserMode == FileBrowserMode.CHAPTERS) {
                    loadChapters(success.project, preserveIndex = true, emitEmptyState = false)
                } else {
                    refreshNonChapterFile(success)
                }
            }
        }
    }

    fun saveEditorScrollPosition(scrollY: Int, selectionStart: Int) {
        lastScrollY = scrollY
        lastSelectionStart = selectionStart
        val state = _uiState.value as? WritingUiState.Success ?: return
        _uiState.value = state.copy(editorScrollY = scrollY, editorSelectionStart = selectionStart)
    }

    fun addReference(filePath: String, selectedText: String, bodyStartLine: Int, bodyEndLine: Int) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        val blocks = state.referenceBlocks
        if (blocks.size >= 5) return

        viewModelScope.launch {
            val fullContent = projectManagementUseCase.readFileContent(filePath).getOrDefault("")
            val fmOffset = calculateFrontMatterLineCount(fullContent)
            val fileStartLine = bodyStartLine + fmOffset + 1
            val fileEndLine = bodyEndLine + fmOffset + 1

            val lines = selectedText.lines()
            val preview = if (lines.size > 2) {
                lines.take(2).joinToString("\n") + "..."
            } else {
                lines.joinToString("\n")
            }

            val block = ReferenceBlock(
                id = java.util.UUID.randomUUID().toString(),
                filePath = filePath.removePrefix(state.project.storagePath + "/"),
                contentPreview = preview,
                content = selectedText,
                startLine = fileStartLine,
                endLine = fileEndLine
            )
            _uiState.value = state.copy(referenceBlocks = blocks + block)
            setSelectedTab(1)
        }
    }

    fun removeReference(id: String) {
        val state = _uiState.value as? WritingUiState.Success ?: return
        _uiState.value = state.copy(
            referenceBlocks = state.referenceBlocks.filter { it.id != id }
        )
    }

    fun clearReferences() {
        val state = _uiState.value as? WritingUiState.Success ?: return
        _uiState.value = state.copy(referenceBlocks = emptyList())
    }

    fun buildMessageWithReferences(userInput: String, references: List<ReferenceBlock>): String {
        return ReferenceBlock.buildReferenceText(references, userInput)
    }

    private fun calculateFrontMatterLineCount(fullContent: String): Int {
        val lines = fullContent.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") return 0
        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (endIndex == -1) return 0
        return endIndex + 2
    }

    private suspend fun refreshNonChapterFile(state: WritingUiState.Success) {
        val dirPath = File(state.project.storagePath, state.fileBrowserMode.dirName()).absolutePath
        val tree = projectManagementUseCase.getFileTree(dirPath)

        val currentPath = state.currentFilePath
        if (currentPath != null) {
            val file = File(currentPath)
            if (file.exists() && file.lastModified() > state.fileLastModified) {
                val content = projectManagementUseCase.readFileContent(currentPath).getOrDefault("")
                _uiState.value = state.copy(
                    fileTree = tree,
                    editorContent = content,
                    wordCount = countWords(content),
                    isDirty = false,
                    fileLastModified = file.lastModified()
                )
            } else {
                _uiState.value = state.copy(fileTree = tree)
            }
        } else {
            _uiState.value = state.copy(fileTree = tree)
        }
    }

    fun retry() {
        loadCurrentProject()
    }

    companion object {
        private var lastSelectedTab: Int = 0
        private var lastProjectId: String? = null
        private var lastScrollY: Int = 0
        private var lastSelectionStart: Int = 0
        private var lastChapterIndex: Int = 0
        private var lastCurrentFilePath: String? = null
        private var lastFileBrowserMode: FileBrowserMode = FileBrowserMode.CHAPTERS
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
