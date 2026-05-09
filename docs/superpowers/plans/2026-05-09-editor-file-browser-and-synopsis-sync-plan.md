# Editor File Browser & Synopsis Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add folder-switching navigation to the editor sidebar for browsing non-chapter project files, with tree-view collapse, file editing, CRUD operations, and long-press context menus. Simultaneously migrate project synopsis from `简介.md` to `info.json`, sync it bidirectionally with the database, and inject it into the AI assistant prompt.

**Architecture:** Extend `WritingViewModel` with file-browsing state and methods. Replace `ChapterListPanel` with a generic `FileListPanel` that switches between chapter mode and file-tree mode via `FileBrowserMode` enum. Add `description` field to `info.json` I/O, pass it through `PromptManager` → `SessionManager` → `IChatService` → `AIChatService` → `AiChatViewModel`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Coroutines, Flow

---

## File Structure Map

| File | Action | Responsibility |
|------|--------|---------------|
| `util/FileManager.kt` | Modify | New: `getFileTree()`, `listFilesInDir()`. Modify: `createInfoJson()`, `readInfoJson()`→`InfoJsonData`, `createProjectDirectoryStructure()`, `createDirectoryStructureAt()` |
| `domain/usecase/ProjectManagementUseCase.kt` | Modify | `createProject()`, `updateProject()`, `exportProjectAsZip()` → pass `description`; new: `getFileTree()`, `createFile()`, `deleteFileOrDir()`, `renameFile()` |
| `util/PromptManager.kt` | Modify | `getNovelWritingAssistantPrompt()` → add `description` parameter |
| `data/remote/SessionManager.kt` | Modify | `buildSystemPrompt()`, `createSessionWithProjectInfo()` → pass `description` |
| `data/remote/IChatService.kt` | Modify | `createSessionWithProjectInfo()` → add `projectDescription` parameter |
| `data/remote/AIChatService.kt` | Modify | `createSessionWithProjectInfo()` → pass through |
| `presentation/viewmodel/AiChatViewModel.kt` | Modify | Pass `project.description` to service |
| `presentation/viewmodel/WritingViewModel.kt` | Modify | Add `FileBrowserMode`, `FileTreeItem`, file-browsing state & methods |
| `presentation/ui/screens/WritingScreen.kt` | Modify | `ChapterListPanel` → `FileListPanel`; add dialogs for delete, rename, new file/folder, edit meta, dropdown menus |
| `app/src/main/assets/prompts/novel_writing_assistant.md` | Modify | Add `{{description}}` placeholder, remove `简介.md` references |
| `AGENTS.md` | Modify | Update directory structure and info.json description |
| `res/values/strings.xml` | Modify | Add 20+ new string resources |
| `res/values-zh-rCN/strings.xml` | Modify | Add Chinese translations |
| `res/values-zh-rTW/strings.xml` | Modify | Add Traditional Chinese translations |

---

## Task 1: Add description to info.json (FileManager)

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/util/FileManager.kt:89-101, 353-377`
- Modify: `app/src/main/java/com/universe_st/quickwriter/util/FileManager.kt:72, 346` (remove 简介.md creation)

- [ ] **Step 1: Update `InfoJsonData` data class to include description**

Modify `FileManager.kt` line 353-358:

```kotlin
data class InfoJsonData(
    val title: String,
    val author: String,
    val genre: String,
    val description: String,
    val createdTime: Long
)
```

- [ ] **Step 2: Update `createInfoJson()` to accept and write description**

Modify `FileManager.kt` line 89-101:

```kotlin
fun createInfoJson(projectDir: File, title: String, author: String, genre: String, description: String, createdTime: Long) {
    val infoFile = File(projectDir, "info.json")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val json = JSONObject().apply {
        put("title", title)
        put("author", author)
        put("genre", genre)
        put("description", description)
        put("createdTime", dateFormat.format(Date(createdTime)))
        put("version", "1.0")
    }
    infoFile.writeText(json.toString(2), Charsets.UTF_8)
}
```

- [ ] **Step 3: Update `readInfoJson()` to parse description (backward compat)**

Modify `FileManager.kt` line 360-377:

```kotlin
fun readInfoJson(projectDir: File): InfoJsonData? {
    val infoFile = File(projectDir, "info.json")
    if (!infoFile.exists()) return null
    return try {
        val json = JSONObject(infoFile.readText(Charsets.UTF_8))
        val title = json.getString("title")
        val author = json.getString("author")
        val genre = json.getString("genre")
        val description = json.optString("description", "")
        val createdTimeStr = json.getString("createdTime")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val createdTime = dateFormat.parse(createdTimeStr)?.time ?: System.currentTimeMillis()
        InfoJsonData(title, author, genre, description, createdTime)
    } catch (e: Exception) {
        Timber.tag("ImportProject").w(e, "Failed to parse info.json")
        null
    }
}
```

- [ ] **Step 4: Remove `createIntroFile()` call from `createProjectDirectoryStructure()`**

Modify `FileManager.kt` line 72 — delete this line:
```kotlin
createIntroFile(projectDir)
```

- [ ] **Step 5: Remove `简介.md` from `createDirectoryStructureAt()`**

Modify `FileManager.kt` line 346 — delete this line:
```kotlin
File(projectDir, "简介.md").createNewFile()
```

- [ ] **Step 6: Remove or deprecate `createIntroFile()` method**

Modify `FileManager.kt` line 81-86 — either delete the method or keep it but ensure it is never called. Since we removed all call sites (Steps 4 & 5), it is safe to delete:

Delete lines 81-86:
```kotlin
private fun createIntroFile(projectDir: File) {
    val introFile = File(projectDir, "简介.md")
    if (!introFile.exists()) {
        introFile.createNewFile()
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/FileManager.kt
git commit -m "feat: add description to info.json, stop creating 简介.md"
```

---

## Task 2: Add getFileTree and file operations to FileManager

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/util/FileManager.kt` (add new methods)
- Modify: `app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt` (add wrapping methods)

- [ ] **Step 1: Add `FileTreeItem` data class and `getFileTree()` to FileManager**

Add at end of `FileManager.kt` (before the closing `}`):

```kotlin
data class FileTreeItem(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val children: List<FileTreeItem> = emptyList(),
    val size: Long = 0
)

fun getFileTree(directory: File): List<FileTreeItem> {
    if (!directory.exists() || !directory.isDirectory) return emptyList()
    val rootPath = directory.absolutePath

    fun buildTree(dir: File, rootLen: Int): List<FileTreeItem> {
        val items = mutableListOf<FileTreeItem>()
        val files = dir.listFiles() ?: return items

        val (dirs, regularFiles) = files.partition { it.isDirectory }

        for (d in dirs.sortedByDescending { it.lastModified() }) {
            val absPath = d.absolutePath
            val relPath = if (absPath.length > rootLen) absPath.substring(rootLen + 1) else d.name
            items.add(FileTreeItem(
                name = d.name,
                relativePath = relPath,
                absolutePath = absPath,
                isDirectory = true,
                lastModified = d.lastModified(),
                children = buildTree(d, rootLen)
            ))
        }

        for (f in regularFiles.sortedByDescending { it.lastModified() }) {
            val absPath = f.absolutePath
            val relPath = if (absPath.length > rootLen) absPath.substring(rootLen + 1) else f.name
            if (f.name.startsWith(".")) continue
            items.add(FileTreeItem(
                name = f.name,
                relativePath = relPath,
                absolutePath = absPath,
                isDirectory = false,
                lastModified = f.lastModified(),
                size = f.length()
            ))
        }

        return items
    }

    return buildTree(directory, rootPath.length)
}
```

- [ ] **Step 2: Add `createFile()` and `createDir()` methods to FileManager**

Add to `FileManager.kt`:

```kotlin
fun createFile(filePath: String): Result<String> {
    return try {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        if (file.createNewFile()) {
            Result.success(file.absolutePath)
        } else {
            Result.failure(IOException("File already exists: $filePath"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun createDirectory(dirPath: String): Result<String> {
    return try {
        val dir = File(dirPath)
        if (dir.mkdirs()) {
            Result.success(dir.absolutePath)
        } else if (dir.exists() && dir.isDirectory) {
            Result.success(dir.absolutePath)
        } else {
            Result.failure(IOException("Cannot create directory: $dirPath"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 3: Add `deleteFileOrDirectory()` method to FileManager**

Add to `FileManager.kt`:

```kotlin
fun deleteFileOrDirectory(path: String): Result<Unit> {
    return try {
        val file = File(path)
        if (!file.exists()) {
            Result.failure(IOException("Path not found: $path"))
        } else {
            file.deleteRecursively()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Add wrapping methods to `ProjectManagementUseCase`**

Add to `ProjectManagementUseCase.kt` (before `getProjectDirectory`):

```kotlin
fun getFileTree(directoryPath: String): List<FileTreeItem> {
    return fileManager.getFileTree(File(directoryPath))
}

suspend fun createFileInProject(filePath: String): Result<String> {
    return fileManager.createFile(filePath)
}

suspend fun createDirectoryInProject(dirPath: String): Result<String> {
    return fileManager.createDirectory(dirPath)
}

suspend fun deleteFileOrDir(path: String): Result<Unit> {
    return fileManager.deleteFileOrDirectory(path)
}

suspend fun renameFileOrDir(oldPath: String, newName: String): Result<String> {
    val oldFile = File(oldPath)
    val newFile = File(oldFile.parentFile, newName)
    return try {
        if (!oldFile.exists()) throw IOException("File not found: $oldPath")
        if (newFile.exists()) throw IOException("Target name already exists: $newName")
        if (oldFile.renameTo(newFile)) {
            Result.success(newFile.absolutePath)
        } else {
            Result.failure(IOException("Rename failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

Add import at top of `ProjectManagementUseCase.kt`:
```kotlin
import com.universe_st.quickwriter.util.FileTreeItem
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/util/FileManager.kt app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt
git commit -m "feat: add getFileTree and file CRUD operations"
```

---

## Task 3: Sync info.json on project update

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt:76, 82-113, 319`
- Modify: `app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt:288` (import path)

- [ ] **Step 1: Update `createProject()` to pass description to `createInfoJson()`**

Modify `ProjectManagementUseCase.kt` line 76:

```kotlin
fileManager.createInfoJson(projectDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
```

- [ ] **Step 2: Add info.json sync to `updateProject()`**

Modify `ProjectManagementUseCase.kt` lines 104-112 — wrap the return with info.json sync:

```kotlin
return projectRepository.updateProject(
    id = id,
    title = title.trim(),
    author = author.trim(),
    genre = genre,
    description = description?.trim(),
    coverImagePath = coverImagePath,
    currentProject = currentProject
).also { result ->
    if (result.isSuccess && currentProject.storagePath.isNotBlank()) {
        val updated = result.getOrNull()
        if (updated != null) {
            fileManager.createInfoJson(
                File(currentProject.storagePath),
                updated.title,
                updated.author,
                updated.genre,
                updated.description ?: "",
                updated.createdTime
            )
        }
    }
}
```

- [ ] **Step 3: Update `exportProjectAsZip()` to pass description**

Modify `ProjectManagementUseCase.kt` line 319:

```kotlin
fileManager.createInfoJson(projectDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
```

- [ ] **Step 4: Update `importProjectFromZip()` to pass description to `createInfoJson()`**

Modify `ProjectManagementUseCase.kt` line 288:

```kotlin
fileManager.createInfoJson(targetDir, project.title, project.author, project.genre, project.description ?: "", project.createdTime)
```

- [ ] **Step 5: Update `importProjectFromZip()` to use description from info.json**

Modify `ProjectManagementUseCase.kt` line 274 — change `description = null` to:

```kotlin
description = infoData.description.ifBlank { null },
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/domain/usecase/ProjectManagementUseCase.kt
git commit -m "feat: sync info.json on project create/update/export/import"
```

---

## Task 4: Update prompt template and PromptManager

**Files:**
- Modify: `app/src/main/assets/prompts/novel_writing_assistant.md`
- Modify: `app/src/main/java/com/universe_st/quickwriter/util/PromptManager.kt:49-63`

- [ ] **Step 1: Add `{{description}}` to prompt template**

Modify `novel_writing_assistant.md` "## 项目信息" section, add after the `- **类型**：{{genre}}` line:

```markdown
- **简介**：{{description}}
```

- [ ] **Step 2: Remove `简介.md` from prompt directory structure**

In `novel_writing_assistant.md`, remove the `├── 简介.md` line from the directory tree. Currently the prompt has no `简介.md` in the tree (verified from reading), but let's double-check. Actually, looking at the file content, the directory tree section does NOT include 简介.md — it only shows `正文/`, `设定/`, `时间线/`, `记录/`, `配置/`. So no change needed for this.

- [ ] **Step 3: Update `getNovelWritingAssistantPrompt()`**

Modify `PromptManager.kt` line 49-63:

```kotlin
fun getNovelWritingAssistantPrompt(
    title: String,
    author: String,
    genre: String,
    storagePath: String,
    description: String = "",
    writingRules: String = ""
): String {
    return resolve("novel_writing_assistant", mapOf(
        "title" to title,
        "author" to author,
        "genre" to genre,
        "description" to description,
        "storagePath" to storagePath,
        "writingRulesContent" to writingRules
    ))
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/prompts/novel_writing_assistant.md app/src/main/java/com/universe_st/quickwriter/util/PromptManager.kt
git commit -m "feat: inject project description into AI system prompt"
```

---

## Task 5: Thread description through session creation chain

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/data/remote/IChatService.kt:10-17`
- Modify: `app/src/main/java/com/universe_st/quickwriter/data/remote/SessionManager.kt:98-113, 329-331`
- Modify: `app/src/main/java/com/universe_st/quickwriter/data/remote/AIChatService.kt:108-119`
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/AiChatViewModel.kt:161-164`

- [ ] **Step 1: Update `IChatService` interface**

Modify `IChatService.kt` line 10-17:

```kotlin
fun createSessionWithProjectInfo(
    projectId: String,
    projectTitle: String,
    projectAuthor: String,
    projectGenre: String,
    projectDescription: String,
    storagePath: String,
    modelConfigId: Int?
): String
```

- [ ] **Step 2: Update `SessionManager.createSessionWithProjectInfo()`**

Modify `SessionManager.kt` line 98-113:

```kotlin
fun createSessionWithProjectInfo(
    projectId: String,
    projectTitle: String,
    projectAuthor: String,
    projectGenre: String,
    projectDescription: String,
    storagePath: String,
    modelConfigId: Int?
): String {
    val writingRules = try {
        val rulesFile = File(storagePath, "配置${File.separator}写作规范.md")
        if (rulesFile.exists()) rulesFile.readText(Charsets.UTF_8).trim() else ""
    } catch (e: Exception) {
        Timber.w(e, "Failed to read writing rules for project %s", projectId)
        ""
    }
    val systemPrompt = buildSystemPrompt(projectTitle, projectAuthor, projectGenre, projectDescription, storagePath, writingRules)
    val resolvedModelConfigId = modelConfigId ?: 0
    // ... rest unchanged
```

- [ ] **Step 3: Update `SessionManager.buildSystemPrompt()`**

Modify `SessionManager.kt` line 329-331:

```kotlin
fun buildSystemPrompt(title: String, author: String, genre: String, description: String, storagePath: String, writingRules: String = ""): String {
    return promptManager.getNovelWritingAssistantPrompt(title, author, genre, storagePath, description, writingRules)
}
```

- [ ] **Step 4: Update `AIChatService.createSessionWithProjectInfo()`**

Modify `AIChatService.kt` line 108-119:

```kotlin
override fun createSessionWithProjectInfo(
    projectId: String,
    projectTitle: String,
    projectAuthor: String,
    projectGenre: String,
    projectDescription: String,
    storagePath: String,
    modelConfigId: Int?
): String {
    return sessionManager.createSessionWithProjectInfo(
        projectId, projectTitle, projectAuthor, projectGenre, projectDescription, storagePath, modelConfigId
    )
}
```

- [ ] **Step 5: Update `AiChatViewModel` caller**

Modify `AiChatViewModel.kt` line 161-164:

```kotlin
service.createSessionWithProjectInfo(
    projectId, project.title, project.author,
    project.genre, project.description ?: "", project.storagePath, config.id
)
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/data/remote/IChatService.kt app/src/main/java/com/universe_st/quickwriter/data/remote/SessionManager.kt app/src/main/java/com/universe_st/quickwriter/data/remote/AIChatService.kt app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/AiChatViewModel.kt
git commit -m "feat: thread project description through session creation chain"
```

---

## Task 6: Add FileBrowserMode, FileTreeItem, and file-browsing state to WritingViewModel

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/WritingViewModel.kt`

- [ ] **Step 1: Add new data classes and enum to WritingViewModel.kt**

Add after the `ChapterFileInfo` data class (line 28):

```kotlin
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

data class DeleteConfirmData(
    val name: String,
    val path: String,
    val isChapter: Boolean,
    val chapterIndex: Int = -1
)

data class RenameDialogData(
    val oldPath: String,
    val oldName: String,
    val isChapter: Boolean,
    val chapterIndex: Int = -1
)

data class NewItemDialogData(
    val parentDir: String,
    val mode: FileBrowserMode
)
```

- [ ] **Step 2: Add file-browsing fields to `WritingUiState.Success`**

Modify `WritingUiState.Success` (line 34-46):

```kotlin
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
    // NEW: file browsing
    val fileBrowserMode: FileBrowserMode = FileBrowserMode.CHAPTERS,
    val fileTree: List<FileTreeItem> = emptyList(),
    val expandedFolders: Set<String> = emptySet(),
    val currentFilePath: String? = null
) : WritingUiState()
```

Add import:
```kotlin
import com.universe_st.quickwriter.util.FileTreeItem
```

- [ ] **Step 3: Fix `updateEditorContent` for non-chapter mode**

The existing `updateEditorContent()` (line 291-301) has `if (state.currentChapterIndex < 0) return` which blocks editing when no chapter is selected (possible in non-chapter mode). Change the guard to allow editing non-chapter files:

```kotlin
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
```

- [ ] **Step 4: Add file-browsing methods to WritingViewModel**

Add the following methods inside `WritingViewModel` class body (after `deleteChapter` at line 379):

```kotlin
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
        _uiState.value = state.copy(
            currentFilePath = file.absolutePath,
            editorContent = content,
            wordCount = countWords(content),
            isDirty = false,
            saveMessage = null
        )
    }
}

fun saveCurrentFile() {
    val state = _uiState.value as? WritingUiState.Success ?: return
    val filePath = state.currentFilePath ?: return
    viewModelScope.launch {
        _uiState.value = state.copy(isSaving = true)
        val result = projectManagementUseCase.writeFileContent(filePath, state.editorContent)
        val current = _uiState.value as? WritingUiState.Success ?: return@launch
        if (result.isSuccess) {
            val stillDirty = current.editorContent != state.editorContent
            _uiState.value = current.copy(isSaving = false, isDirty = stillDirty, saveMessage = "已保存")
            clearSaveMessageAfterDelay()
        } else {
            _uiState.value = current.copy(isSaving = false, saveMessage = "保存失败")
            clearSaveMessageAfterDelay()
        }
    }
}

fun setDeleteConfirm(data: DeleteConfirmData) {
    val state = _uiState.value as? WritingUiState.Success ?: return
    // Store in local screen state — handled in WritingScreen via callback
    // We'll use a simpler approach: emit event via state
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
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/viewmodel/WritingViewModel.kt
git commit -m "feat: add file browsing state and methods to WritingViewModel"
```

---

## Task 7: Add string resources for new UI

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add English strings to `res/values/strings.xml`**

Add after the last `writing_` string (after line 122):

```xml
<string name="writing_browse_chapters">Chapters</string>
<string name="writing_browse_settings">Settings</string>
<string name="writing_browse_timeline">Timeline</string>
<string name="writing_browse_logs">Logs</string>
<string name="writing_browse_config">Config</string>
<string name="writing_file_count">%1$d files</string>
<string name="writing_no_files">No files</string>
<string name="writing_delete_confirm_title">Confirm Delete</string>
<string name="writing_delete_confirm_message">Delete \"%1$s\"? This cannot be undone.</string>
<string name="writing_rename_title">Rename</string>
<string name="writing_rename_file">Rename File</string>
<string name="writing_edit_meta">Edit Metadata</string>
<string name="writing_edit_meta_title">Edit Chapter Metadata</string>
<string name="writing_field_title">Title</string>
<string name="writing_field_volume">Volume</string>
<string name="writing_field_summary">Summary</string>
<string name="writing_field_order">Order</string>
<string name="writing_field_file_name">File Name</string>
<string name="writing_field_folder_name">Folder Name</string>
<string name="writing_add_file">Add File</string>
<string name="writing_add_folder">Add Folder</string>
<string name="writing_new_file_title">New File</string>
<string name="writing_new_folder_title">New Folder</string>
<string name="writing_editor_editing_file">Editing: %1$s</string>
<string name="writing_editor_editing_chapter">Editing: Chapter</string>
<string name="error_invalid_file_name">Invalid file name. Avoid: / \\ : * ? \" &lt; &gt; |</string>
<string name="error_file_name_exists">A file with this name already exists</string>
<string name="error_folder_not_empty">Folder is not empty</string>
<string name="writing_save">Save</string>
```

- [ ] **Step 2: Add Simplified Chinese strings to `res/values-zh-rCN/strings.xml`**

Add after the last `writing_` string:

```xml
<string name="writing_browse_chapters">章节</string>
<string name="writing_browse_settings">设定</string>
<string name="writing_browse_timeline">时间线</string>
<string name="writing_browse_logs">记录</string>
<string name="writing_browse_config">配置</string>
<string name="writing_file_count">%1$d 个文件</string>
<string name="writing_no_files">暂无文件</string>
<string name="writing_delete_confirm_title">确认删除</string>
<string name="writing_delete_confirm_message">确定删除 \"%1$s\"？此操作不可撤销。</string>
<string name="writing_rename_title">重命名</string>
<string name="writing_rename_file">修改文件名</string>
<string name="writing_edit_meta">修改元数据</string>
<string name="writing_edit_meta_title">修改章节元数据</string>
<string name="writing_field_title">标题</string>
<string name="writing_field_volume">分卷</string>
<string name="writing_field_summary">摘要</string>
<string name="writing_field_order">序号</string>
<string name="writing_field_file_name">文件名</string>
<string name="writing_field_folder_name">文件夹名</string>
<string name="writing_add_file">添加文件</string>
<string name="writing_add_folder">添加文件夹</string>
<string name="writing_new_file_title">新建文件</string>
<string name="writing_new_folder_title">新建文件夹</string>
<string name="writing_editor_editing_file">正在编辑: %1$s</string>
<string name="writing_editor_editing_chapter">正在编辑: 章节</string>
<string name="error_invalid_file_name">文件名不合法，不能包含: / \\ : * ? \" &lt; &gt; |</string>
<string name="error_file_name_exists">已存在同名文件</string>
<string name="error_folder_not_empty">文件夹非空</string>
<string name="writing_save">保存</string>
```

- [ ] **Step 3: Add Traditional Chinese strings to `res/values-zh-rTW/strings.xml`**

Add after the last `writing_` string:

```xml
<string name="writing_browse_chapters">章節</string>
<string name="writing_browse_settings">設定</string>
<string name="writing_browse_timeline">時間線</string>
<string name="writing_browse_logs">記錄</string>
<string name="writing_browse_config">配置</string>
<string name="writing_file_count">%1$d 個檔案</string>
<string name="writing_no_files">暫無檔案</string>
<string name="writing_delete_confirm_title">確認刪除</string>
<string name="writing_delete_confirm_message">確定刪除 \"%1$s\"？此操作不可撤銷。</string>
<string name="writing_rename_title">重新命名</string>
<string name="writing_rename_file">修改檔案名稱</string>
<string name="writing_edit_meta">修改元資料</string>
<string name="writing_edit_meta_title">修改章節元資料</string>
<string name="writing_field_title">標題</string>
<string name="writing_field_volume">分卷</string>
<string name="writing_field_summary">摘要</string>
<string name="writing_field_order">序號</string>
<string name="writing_field_file_name">檔案名稱</string>
<string name="writing_field_folder_name">資料夾名稱</string>
<string name="writing_add_file">新增檔案</string>
<string name="writing_add_folder">新增資料夾</string>
<string name="writing_new_file_title">新增檔案</string>
<string name="writing_new_folder_title">新增資料夾</string>
<string name="writing_editor_editing_file">正在編輯: %1$s</string>
<string name="writing_editor_editing_chapter">正在編輯: 章節</string>
<string name="error_invalid_file_name">檔案名稱不合法，不能包含: / \\ : * ? \" &lt; &gt; |</string>
<string name="error_file_name_exists">已存在同名檔案</string>
<string name="error_folder_not_empty">資料夾非空</string>
<string name="writing_save">儲存</string>
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "feat: add i18n strings for file browser UI"
```

---

## Task 8: Implement FileListPanel and dialog composables in WritingScreen

**Files:**
- Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt`

This is the largest task. We will refactor `ChapterListPanel` into `FileListPanel`, add all new dialog composables, and wire up the new state.

- [ ] **Step 1: Add new imports at top of WritingScreen.kt**

Add after existing imports (line 44):

```kotlin
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.ui.text.style.TextAlign
import com.universe_st.quickwriter.presentation.viewmodel.DeleteConfirmData
import com.universe_st.quickwriter.presentation.viewmodel.FileBrowserMode
import com.universe_st.quickwriter.presentation.viewmodel.NewItemDialogData
import com.universe_st.quickwriter.presentation.viewmodel.RenameDialogData
import com.universe_st.quickwriter.util.FileTreeItem
import com.universe_st.quickwriter.util.ChapterMeta
```

- [ ] **Step 2: Refactor `EditorContent` to accept new callbacks and file mode state**

Replace `EditorContent` (lines 331-443) with:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorContent(
    state: WritingUiState.Success,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onDeleteChapter: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    onBrowseModeChange: (FileBrowserMode) -> Unit,
    onSelectNonChapterFile: (FileTreeItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onFileDeleteRequest: (String, String, Boolean, Int) -> Unit,
    onFileRenameRequest: (String, String, Boolean, Int) -> Unit,
    onCreateNewFile: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onEditChapterMeta: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val editorConfig = remember(isDark) { AppEditorConfig(isDark = isDark) }
    val isChapterMode = state.fileBrowserMode == FileBrowserMode.CHAPTERS
    val hasContent = if (isChapterMode) state.chapters.isNotEmpty()
                     else state.fileTree.isNotEmpty()

    if (!hasContent && isChapterMode && state.fileBrowserMode == FileBrowserMode.CHAPTERS) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.writing_no_chapters),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCreateChapter) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.writing_create_first_chapter))
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showChapterList,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                Box(
                    modifier = Modifier.pointerInput(showChapterList) {
                        if (!showChapterList) return@pointerInput
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                                if (totalDrag < -100f) {
                                    totalDrag = 0f
                                    onToggleChapterList()
                                }
                            }
                        )
                    }
                ) {
                    FileListPanel(
                        state = state,
                        onSelectChapter = onSelectChapter,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onCreateChapter = onCreateChapter,
                        onDeleteChapter = onDeleteChapter,
                        onBrowseModeChange = onBrowseModeChange,
                        onSelectNonChapterFile = onSelectNonChapterFile,
                        onToggleFolder = onToggleFolder,
                        onFileDeleteRequest = onFileDeleteRequest,
                        onFileRenameRequest = onFileRenameRequest,
                        onCreateNewFile = onCreateNewFile,
                        onCreateNewFolder = onCreateNewFolder,
                        onEditChapterMeta = onEditChapterMeta,
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )
                }
            }

            if (showChapterList) { VerticalDivider() }

            val showEditor = if (isChapterMode) state.currentChapterIndex >= 0
                             else state.currentFilePath != null

            if (showEditor || !showChapterList) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (showChapterList) Modifier.pointerInput(showChapterList) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    onToggleChapterList()
                                }
                            } else Modifier
                        )
                ) {
                    if (showEditor) {
                        MarkorEditor(
                            value = state.editorContent,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            editorConfig = editorConfig,
                            highlightingMode = HighlightingMode.MARKDOWN,
                            enabled = true
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.writing_no_files),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Replace `ChapterListPanel` with `FileListPanel`**

Replace the entire `ChapterListPanel` composable (lines 445-515) and `ChapterListItem` (lines 517-616) with the new `FileListPanel` and supporting composables:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListPanel(
    state: WritingUiState.Success,
    onSelectChapter: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onDeleteChapter: (Int) -> Unit,
    onBrowseModeChange: (FileBrowserMode) -> Unit,
    onSelectNonChapterFile: (FileTreeItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onFileDeleteRequest: (String, String, Boolean, Int) -> Unit,
    onFileRenameRequest: (String, String, Boolean, Int) -> Unit,
    onCreateNewFile: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onEditChapterMeta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChapterMode = state.fileBrowserMode == FileBrowserMode.CHAPTERS
    var showModeDropdown by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showChapterContextMenu by remember { mutableStateOf<Int?>(null) }
    var showFileContextMenu by remember { mutableStateOf<FileTreeItem?>(null) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Header row: mode label with dropdown + add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode label + dropdown trigger
            Box {
                TextButton(
                    onClick = { showModeDropdown = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(state.fileBrowserMode.displayNameResId()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showModeDropdown,
                    onDismissRequest = { showModeDropdown = false }
                ) {
                    FileBrowserMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(mode.displayNameResId())) },
                            onClick = {
                                showModeDropdown = false
                                onBrowseModeChange(mode)
                            },
                            leadingIcon = if (mode == state.fileBrowserMode) {
                                { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Add button
            Box {
                FilledTonalIconButton(
                    onClick = {
                        if (isChapterMode) onCreateChapter()
                        else showAddMenu = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(
                            if (isChapterMode) R.string.writing_new_chapter
                            else R.string.writing_add_file
                        ),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!isChapterMode) {
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_add_file)) },
                            onClick = { showAddMenu = false; onCreateNewFile() },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_add_folder)) },
                            onClick = { showAddMenu = false; onCreateNewFolder() },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        if (isChapterMode) {
            // CHAPTER MODE: existing chapter list
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.chapters) { index, chapter ->
                    val isSelected = index == state.currentChapterIndex
                    ChapterListItem(
                        chapter = chapter,
                        isSelected = isSelected,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.chapters.size - 1,
                        onClick = { onSelectChapter(index) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onDelete = { onFileDeleteRequest(chapter.fileName, chapter.filePath, true, index) },
                        onLongPress = { showChapterContextMenu = index }
                    )
                }
            }

            // Chapter long-press context menu
            showChapterContextMenu?.let { idx ->
                val chapter = state.chapters.getOrNull(idx) ?: return@let
                AlertDialog(
                    onDismissRequest = { showChapterContextMenu = null },
                    title = { Text(chapter.title) },
                    text = {
                        Column {
                            TextButton(onClick = {
                                showChapterContextMenu = null
                                onFileRenameRequest(chapter.filePath, chapter.fileName, true, idx)
                            }) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.writing_rename_file))
                            }
                            TextButton(onClick = {
                                showChapterContextMenu = null
                                onEditChapterMeta(idx)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.writing_edit_meta))
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showChapterContextMenu = null }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

        } else {
            // FILE MODE: tree view
            if (state.fileTree.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.writing_no_files),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.fileTree, key = { it.relativePath }) { item ->
                        FileTreeItemNode(
                            item = item,
                            depth = 0,
                            isExpanded = state.expandedFolders.contains(item.relativePath),
                            isSelected = state.currentFilePath == item.absolutePath,
                            onToggleFolder = onToggleFolder,
                            onSelectFile = onSelectNonChapterFile,
                            onDelete = { onFileDeleteRequest(item.name, item.absolutePath, false, -1) },
                            onLongPress = { showFileContextMenu = item }
                        )
                    }
                }
            }

            // File long-press context menu
            showFileContextMenu?.let { item ->
                AlertDialog(
                    onDismissRequest = { showFileContextMenu = null },
                    title = { Text(item.name) },
                    text = {
                        TextButton(onClick = {
                            showFileContextMenu = null
                            onFileRenameRequest(item.absolutePath, item.name, false, -1)
                        }) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.writing_rename_file))
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showFileContextMenu = null }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
        }

        HorizontalDivider()

        // Footer: count
        Text(
            text = if (isChapterMode) {
                stringResource(R.string.writing_chapter_count, state.chapters.size)
            } else {
                stringResource(R.string.writing_file_count, countFilesInTree(state.fileTree))
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun countFilesInTree(tree: List<FileTreeItem>): Int {
    var count = 0
    for (item in tree) {
        if (!item.isDirectory) count++
        count += countFilesInTree(item.children)
    }
    return count
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeItemNode(
    item: FileTreeItem,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleFolder: (String) -> Unit,
    onSelectFile: (FileTreeItem) -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else Color.Transparent

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(
                    onClick = { onSelectFile(item) },
                    onLongClick = onLongPress
                )
                .padding(start = (8 + depth * 12).dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isDirectory) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextSecondary
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }

            Icon(
                if (item.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp),
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                       else TextSecondary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }

        // Render children if expanded
        if (item.isDirectory && isExpanded) {
            item.children.forEach { child ->
                FileTreeItemNode(
                    item = child,
                    depth = depth + 1,
                    isExpanded = false, // children start collapsed
                    isSelected = false,
                    onToggleFolder = onToggleFolder,
                    onSelectFile = onSelectFile,
                    onDelete = { /* handled by parent's delete logic */ },
                    onLongPress = { /* handled */ }
                )
            }
        }
    }
}
```

- [ ] **Step 4: Add dialog composables at end of WritingScreen.kt**

Add before the final closing `}` of the file (after `NoProjectContent`):

```kotlin
@Composable
fun DeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_delete_confirm_title)) },
        text = {
            Text(stringResource(R.string.writing_delete_confirm_message, name))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun RenameFileDialog(
    oldName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(oldName.removeSuffix(".md")) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_rename_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = validateFileName(it, oldName)
                    },
                    label = { Text(stringResource(R.string.writing_field_file_name)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validateFileName(newName, oldName)
                    if (validated == null || newName == oldName.removeSuffix(".md")) {
                        onConfirm(newName)
                    } else {
                        error = validated
                    }
                },
                enabled = newName.isNotBlank()
            ) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun EditChapterMetaDialog(
    currentMeta: ChapterMeta,
    onConfirm: (ChapterMeta) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentMeta.title) }
    var volume by remember { mutableStateOf(currentMeta.volume) }
    var summary by remember { mutableStateOf(currentMeta.summary) }
    var order by remember { mutableStateOf(currentMeta.order.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_edit_meta_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.writing_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = volume,
                    onValueChange = { volume = it },
                    label = { Text(stringResource(R.string.writing_field_volume)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text(stringResource(R.string.writing_field_summary)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.writing_field_order)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newMeta = ChapterMeta(
                        title = title.ifBlank { currentMeta.title },
                        order = order.toIntOrNull() ?: currentMeta.order,
                        volume = volume,
                        summary = summary
                    )
                    onConfirm(newMeta)
                }
            ) { Text(stringResource(R.string.writing_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_new_file_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        error = validateFileName(it, null)
                    },
                    label = { Text(stringResource(R.string.writing_field_file_name)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validateFileName(fileName, null)
                    if (validated == null && fileName.isNotBlank()) {
                        onConfirm(fileName)
                    } else {
                        error = validated ?: "Name cannot be empty"
                    }
                },
                enabled = fileName.isNotBlank()
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_new_folder_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        error = validateFileName(it, null)
                    },
                    label = { Text(stringResource(R.string.writing_field_folder_name)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validated = validateFileName(folderName, null)
                    if (validated == null && folderName.isNotBlank()) {
                        onConfirm(folderName)
                    } else {
                        error = validated ?: "Name cannot be empty"
                    }
                },
                enabled = folderName.isNotBlank()
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

private fun validateFileName(name: String, oldName: String?): String? {
    if (name.isBlank()) return "Name cannot be empty"
    val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    if (name.any { it in invalidChars }) return "Invalid characters: / \\ : * ? \" < > |"
    if (oldName != null && name == oldName) return null // unchanged
    return null
}
```

- [ ] **Step 5: Update `WritingScreen` composable to wire up new state and dialogs**

Modify the `WritingScreen` function (lines 47-195) to add new dialog state and wire up callbacks:

Replace the `showNewChapterDialog` state (line 54) with:

```kotlin
var showNewChapterDialog by remember { mutableStateOf(false) }
var showChapterList by remember { mutableStateOf(false) }
// New dialog states
var deleteConfirmData by remember { mutableStateOf<DeleteConfirmData?>(null) }
var renameDialogData by remember { mutableStateOf<RenameDialogData?>(null) }
var editMetaChapterIndex by remember { mutableStateOf<Int?>(null) }
var showNewFileDialog by remember { mutableStateOf(false) }
var showNewFolderDialog by remember { mutableStateOf(false) }
```

Replace the `EditorContent` call in the `is WritingUiState.Success ->` block (line 122-132) with:

```kotlin
0 -> EditorContent(
    state = state,
    showChapterList = showChapterList,
    onToggleChapterList = { showChapterList = !showChapterList },
    onSelectChapter = { viewModel.selectChapter(it) },
    onMoveUp = { viewModel.moveChapter(it, it - 1) },
    onMoveDown = { viewModel.moveChapter(it, it + 1) },
    onCreateChapter = { showNewChapterDialog = true },
    onDeleteChapter = { /* now handled via deleteConfirmData */ },
    onContentChange = {
        if (state.fileBrowserMode == FileBrowserMode.CHAPTERS) {
            viewModel.updateEditorContent(it)
        } else {
            // For non-chapter files, track dirty state via direct state update
            val current = viewModel.uiState.value as? WritingUiState.Success ?: return@EditorContent
            viewModel.updateEditorContent(it)
        }
    },
    onBrowseModeChange = { viewModel.switchBrowseMode(it) },
    onSelectNonChapterFile = { viewModel.selectNonChapterFile(it) },
    onToggleFolder = { viewModel.toggleFolderExpanded(it) },
    onFileDeleteRequest = { name, path, isChapter, chapterIndex ->
        deleteConfirmData = DeleteConfirmData(name, path, isChapter, chapterIndex)
    },
    onFileRenameRequest = { oldPath, oldName, isChapter, chapterIndex ->
        renameDialogData = RenameDialogData(oldPath, oldName, isChapter, chapterIndex)
    },
    onCreateNewFile = { showNewFileDialog = true },
    onCreateNewFolder = { showNewFolderDialog = true },
    onEditChapterMeta = { editMetaChapterIndex = it }
)
```

Replace the `if (showNewChapterDialog)` block at the end of `WritingScreen` (lines 164-194) with all dialogs:

```kotlin
// New Chapter Dialog (existing)
if (showNewChapterDialog) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { showNewChapterDialog = false },
        title = { Text(stringResource(R.string.chapter_new_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.chapter_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.createNewChapter(title.trim())
                showNewChapterDialog = false
            }) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = { showNewChapterDialog = false }) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

// Delete Confirm Dialog
deleteConfirmData?.let { data ->
    DeleteConfirmDialog(
        name = data.name,
        onConfirm = {
            if (data.isChapter) {
                viewModel.deleteChapterWithConfirm(data.chapterIndex)
            } else {
                viewModel.deleteFileOrFolder(FileTreeItem(
                    name = data.name,
                    relativePath = "",
                    absolutePath = data.path,
                    isDirectory = false,
                    lastModified = 0
                ))
            }
            deleteConfirmData = null
        },
        onDismiss = { deleteConfirmData = null }
    )
}

// Rename Dialog
renameDialogData?.let { data ->
    RenameFileDialog(
        oldName = data.oldName,
        onConfirm = { newName ->
            viewModel.renameFile(data.oldPath, newName, data.isChapter, data.chapterIndex)
            renameDialogData = null
        },
        onDismiss = { renameDialogData = null }
    )
}

// Edit Chapter Meta Dialog
editMetaChapterIndex?.let { idx ->
    val state = (viewModel.uiState.value as? WritingUiState.Success) ?: return@let
    val chapter = state.chapters.getOrNull(idx) ?: return@let
    EditChapterMetaDialog(
        currentMeta = ChapterMeta(
            title = chapter.title,
            order = chapter.order,
            volume = chapter.volume,
            summary = chapter.summary
        ),
        onConfirm = { meta ->
            viewModel.editChapterMeta(idx, meta)
            editMetaChapterIndex = null
        },
        onDismiss = { editMetaChapterIndex = null }
    )
}

// New File Dialog
if (showNewFileDialog) {
    NewFileDialog(
        onConfirm = { name ->
            viewModel.createNewFileInCurrentDir(name)
            showNewFileDialog = false
        },
        onDismiss = { showNewFileDialog = false }
    )
}

// New Folder Dialog
if (showNewFolderDialog) {
    NewFolderDialog(
        onConfirm = { name ->
            viewModel.createNewFolderInCurrentDir(name)
            showNewFolderDialog = false
        },
        onDismiss = { showNewFolderDialog = false }
    )
}
```

- [ ] **Step 6: Update `WritingTopBar` save button to handle non-chapter mode**

Modify the save button logic in `WritingTopBar` (around line 276-280). The `onSave` callback needs to dispatch to the right save method. In `WritingScreen`, change:

```kotlin
onSave = {
    val s = viewModel.uiState.value as? WritingUiState.Success ?: return@WritingTopBar
    if (s.fileBrowserMode == FileBrowserMode.CHAPTERS) {
        viewModel.saveCurrentChapter()
    } else {
        viewModel.saveCurrentFile()
    }
},
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/WritingScreen.kt
git commit -m "feat: implement FileListPanel with folder browsing, dialogs, and context menus"
```

---

## Task 9: Update AGENTS.md

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Update project directory structure**

Modify the directory structure in AGENTS.md (remove `├── 简介.md`):

```
/{项目ID}/
├── info.json                # 项目元数据（书名、作者、类型、简介、创建时间等）
├── 正文/                    # 小说正文章节
├── 设定/
│   ├── 人物/
│   ├── 地点/
│   ├── 组织/
│   └── 物品/
├── 时间线/
├── 记录/
└── 配置/
    ├── AI指令.md
    └── 写作规范.md
```

- [ ] **Step 2: Update info.json format description**

Modify:

```json
{
  "title": "书名",
  "author": "作者",
  "genre": "类型",
  "description": "小说简介/核心设定（对应数据库 ProjectEntity.description）",
  "createdTime": "2026-04-28T12:00:00.000Z",
  "version": "1.0"
}
```

- [ ] **Step 3: Add description field to key files table**

Add `description` to the info.json field list description.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md
git commit -m "docs: update AGENTS.md for info.json description field"
```

---

## Task 10: Build verification

- [ ] **Step 1: Run debug build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any compilation errors**

If compilation fails, read the error output, fix issues, and re-run.

---

## Implementation Order

Execute tasks in this order: 1 → 2 → 3 → 4 → 5 → 7 → 6 → 8 → 9 → 10

Why: Foundation (info.json + FileManager) first, then prompt chain, then strings, then ViewModel, then UI (the most complex part), then docs, then verification.

---

## Summary of All Changes

| # | Task | Files Changed |
|---|------|--------------|
| 1 | info.json description field | FileManager.kt |
| 2 | FileTree + CRUD operations | FileManager.kt, ProjectManagementUseCase.kt |
| 3 | info.json sync on update | ProjectManagementUseCase.kt |
| 4 | Prompt template + PromptManager | novel_writing_assistant.md, PromptManager.kt |
| 5 | Session creation chain | IChatService.kt, SessionManager.kt, AIChatService.kt, AiChatViewModel.kt |
| 6 | WritingViewModel extensions | WritingViewModel.kt |
| 7 | String resources (i18n) | 3 strings.xml files |
| 8 | FileListPanel + dialogs | WritingScreen.kt |
| 9 | AGENTS.md update | AGENTS.md |
| 10 | Build verification | — |
